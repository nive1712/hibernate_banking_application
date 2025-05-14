package com.bank.dao.impl;

import com.bank.model.BankAccount;
import com.bank.model.CardBlockStatus;

import com.bank.model.TransactionHistory;
import com.bank.model.User;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;


@Repository
@Transactional
public class BankAccountDaoImpl {
	 private static final Logger logger = LogManager.getLogger(BankAccountDaoImpl.class);
	 private static final SecureRandom secureRandom = new SecureRandom();
	 private static final String USER_ID = "userId";
	 public static final String ACCOUNT_NUMBER = "accountNumber";
	 private static final String ERROR_MESSAGE = "An error occurred while updating the bank account.";
	
	 
     private SessionFactory sessionFactory;
		
		@Autowired
	    public void setSessionFactory(SessionFactory sessionFactory) {
	        this.sessionFactory = sessionFactory;
	    }

		 public String getTransactionHistory(int userId) {
		        String transactionHistory;
		        Session session = sessionFactory.getCurrentSession();
		        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		        
		        CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
		        Root<TransactionHistory> root = criteriaQuery.from(TransactionHistory.class);
		        criteriaQuery.select(root.get("transactionHistory"));
		        
		        criteriaQuery.where(criteriaBuilder.equal(root.get(USER_ID), userId));
		        
		        try {
		            transactionHistory = session.createQuery(criteriaQuery).getSingleResult();
		        } catch (NoResultException e) {
		            transactionHistory = "No transactions have been recorded for the user.";
		        }
		        
		        return transactionHistory;
		    }
    public List<TransactionHistory> getTransactionsByBankAccount(String accountNumber, int pageNumber, int pageSize) {
        Session session = sessionFactory.getCurrentSession();
        Query<TransactionHistory> query = session.createQuery(
                "FROM TransactionHistory WHERE bankAccount.accountNumber = :accountNumber ORDER BY date DESC", TransactionHistory.class);
        query.setParameter(ACCOUNT_NUMBER , accountNumber);
        query.setFirstResult((pageNumber - 1) * pageSize);
        query.setMaxResults(pageSize);
        return query.list();
    }
  

    
    public int getUserIdByAccountNumber(String accountNumber) {
        Session session = sessionFactory.openSession();
        Query<Integer> query = session.createQuery("SELECT userId FROM BankAccount WHERE accountNumber = :accountNumber", Integer.class);
        query.setParameter(ACCOUNT_NUMBER , accountNumber);
        Integer userId = query.uniqueResult();
        session.close();
        return userId != null ? userId : -1;
    }

    public Integer getAccountIdByAccountNumber(String accountNumber) {
        Session session = sessionFactory.openSession();
        Query<Integer> query = session.createQuery("SELECT id FROM BankAccount WHERE accountNumber = :accountNumber", Integer.class);
        query.setParameter(ACCOUNT_NUMBER , accountNumber);
        Integer accountId = query.uniqueResult();
        session.close();
        return accountId;
    }


      
    public BankAccount getBankAccountByAccountNumber(String accountNumber) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM BankAccount WHERE accountNumber = :accountNumber", BankAccount.class)
                    .setParameter(ACCOUNT_NUMBER , accountNumber)
                    .uniqueResult();
        } catch (Exception e) {
            // Log error
            return null;
        }
    }

    
    public String generateAccountNumber() {
        return "ACC" + secureRandom.nextInt(1000000000);
    }
    
    public int generateNextTransId() {
        try (Session session = sessionFactory.openSession()) {
            String sql = "SELECT COALESCE(MAX(transid), 0) + 1 FROM bank_account";
            BigInteger maxTransId = (BigInteger) session.createNativeQuery(sql).uniqueResult();
            return maxTransId.intValue();
        } catch (Exception e) {
            logger.error("An error occurred while generating the next transaction ID.", e);
            return 1;
        }
    }

    public BankAccount getBankAccountByUserId(int userId) {
        Session session = sessionFactory.openSession();
        Query<BankAccount> query = session.createQuery("FROM BankAccount WHERE userId = :userId", BankAccount.class);
        query.setParameter(USER_ID, userId);
        BankAccount bankAccount = query.uniqueResult();
        session.close();
        return bankAccount;
    }
    
    public BankAccount getBankAccountByAccountId(int accountId) {
        return sessionFactory.getCurrentSession().createQuery("FROM BankAccount WHERE accountId = :accountId", BankAccount.class)
                .setParameter("accountId", accountId)
                .uniqueResult();
    }
    
   
    
    public boolean createBankAccount(BankAccount bankAccount) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        session.save(bankAccount);
        transaction.commit();
        session.close();
        return true;
    }

    public boolean updateBankAccount(BankAccount bankAccount) {
        try {
            Session session = sessionFactory.getCurrentSession();
            session.update(bankAccount);
            return true;
        } catch (Exception e) {
            logger.error(ERROR_MESSAGE, e);
            return false; 
        }
    }

    
    public String getAccountNumberByUserId(int userId) {
        Session session = sessionFactory.openSession();
        Query<String> query = session.createQuery("SELECT accountNumber FROM BankAccount WHERE userId = :userId", String.class);
        query.setParameter(USER_ID, userId);
        String accountNumber = query.uniqueResult();
        session.close();
        return accountNumber;
    }
    
    public User showCompleteDetails(int accountId, String accountNumber) {
        Session session = sessionFactory.getCurrentSession();
        try {
            String hql = "FROM User u " +
                         "LEFT JOIN FETCH u.bankAccount ba " +
                         "LEFT JOIN FETCH ba.cardBlockStatus " +
                         "LEFT JOIN FETCH u.loan " +
                         "WHERE ba.accountId = :accountId AND ba.accountNumber = :accountNumber";
            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("accountId", accountId);
            query.setParameter(ACCOUNT_NUMBER , accountNumber);
            return query.uniqueResult();
        } catch (Exception e) {
        	logger.error(ERROR_MESSAGE, e);
            return null;
        }
    }

   
    public boolean blockCard(int userId, String accountNumber, int pin, String reason) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            Query<BankAccount> query = session.createQuery(
                "FROM BankAccount WHERE userId = :userId AND accountNumber = :accountNumber AND pin = :pin",
                BankAccount.class);
            query.setParameter(USER_ID, userId);
            query.setParameter(ACCOUNT_NUMBER, accountNumber);
            query.setParameter("pin", pin);

            BankAccount bankAccount = query.uniqueResult();

            if (bankAccount != null) {
                CardBlockStatus cardBlockStatus = new CardBlockStatus(true, reason, LocalDate.now());
                cardBlockStatus.setBankAccount(bankAccount);
                session.save(cardBlockStatus);

                transaction.commit();
                return true;
            } else {
                if (transaction != null) transaction.rollback();
                return false;
            }
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            logger.error("Error blocking card: {}", e.getMessage());
            return false;
        }
    }


   
    public boolean unblockCard(int userId, String accountNumber, int pin) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            Query<BankAccount> query = session.createQuery(
                "FROM BankAccount WHERE userId = :userId AND accountNumber = :accountNumber AND pin = :pin",
                BankAccount.class);
            query.setParameter(USER_ID, userId);
            query.setParameter(ACCOUNT_NUMBER, accountNumber);
            query.setParameter("pin", pin);

            BankAccount bankAccount = query.uniqueResult();

            if (bankAccount != null) {
                Query<CardBlockStatus> updateQuery = session.createQuery(
                    "UPDATE CardBlockStatus SET isCardBlocked = false, unblockDate = :unblockDate WHERE bankAccount = :bankAccount AND isCardBlocked = true");
                updateQuery.setParameter("unblockDate", LocalDate.now());
                updateQuery.setParameter("bankAccount", bankAccount);
                int result = updateQuery.executeUpdate();

                transaction.commit();
                return result > 0;
            } else {
                transaction.rollback();
                return false;
            }
        } catch (Exception e) {
            logger.error("Error unblocking card: {}", e.getMessage());
            return false;
        }
    }

    


    public boolean isCardBlocked(String accountNumber) {
        Session session = sessionFactory.openSession();
        Query<CardBlockStatus> query = session.createQuery(
            "SELECT cbs FROM CardBlockStatus cbs JOIN cbs.bankAccount ba WHERE ba.accountNumber = :accountNumber AND cbs.isCardBlocked = true",
            CardBlockStatus.class
        );
        query.setParameter(ACCOUNT_NUMBER, accountNumber);
        CardBlockStatus cardBlockStatus = query.uniqueResult();
        session.close();
        return cardBlockStatus != null;
    }
    

    public Integer getPinByUserId(int userId) {
        Session session = sessionFactory.openSession();
        Query<Integer> query = session.createQuery("SELECT pin FROM BankAccount WHERE userId = :userId", Integer.class);
        query.setParameter(USER_ID, userId);
        Integer pin = query.uniqueResult();
        session.close();
        return pin;
    }
    
    
    public boolean depositAmount(BigDecimal amount) {
        Session session = sessionFactory.openSession();
        org.hibernate.Transaction transaction = session.beginTransaction();

        try {
            TransactionHistory depositTransaction = new TransactionHistory();
            depositTransaction.setAmount(amount);
            depositTransaction.setDate(LocalDate.now());
            depositTransaction.setType("Deposit");
            depositTransaction.setDescription("Deposit transaction");
            session.save(depositTransaction);

            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error(ERROR_MESSAGE, e);
            return false; 
        } finally {
            session.close();
        }
    }

}

