package com.bank.dao.impl;


import com.bank.model.Loan;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Transactional

public class LoanDaoImpl {
	 private static final String ERROR_MESSAGE = "An error occurred while updating the bank account.";
	 private static final Logger logger = LogManager.getLogger(LoanDaoImpl.class);


    private SessionFactory sessionFactory; 
	
	@Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
   
    public boolean createLoan(Loan loan) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.save(loan);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error( ERROR_MESSAGE , e);
            return false;
        } finally {
            session.close();
        }
    }
    
    public Loan getLoanByUserId(int userId) {
        Session session = null;
        Loan loan = null;
        try {
            session = sessionFactory.openSession();
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Loan> query = builder.createQuery(Loan.class);
            Root<Loan> root = query.from(Loan.class);
            query.select(root).where(builder.equal(root.get("user").get("userId"), userId));
            loan = session.createQuery(query).uniqueResult();
        } catch (Exception e) {
        	logger.error( ERROR_MESSAGE , e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return loan;
    }

    
    public Loan getLoanById(int loanId) {
        Session session = sessionFactory.openSession();
        try {
            return session.get(Loan.class, loanId);
        } catch (Exception e) {
        	logger.error( ERROR_MESSAGE , e);
            return null;
        } finally {
            session.close();
        }
    }
  
    public void updateLoan(Loan loan) {
        sessionFactory.getCurrentSession().update(loan);
    }

    
    public Loan getLoanByAccountId(int accountId) {
        return sessionFactory.getCurrentSession().createQuery("FROM Loan WHERE bankAccountId = :accountId", Loan.class)
                .setParameter("accountId", accountId)
                .uniqueResult();
    }
}
