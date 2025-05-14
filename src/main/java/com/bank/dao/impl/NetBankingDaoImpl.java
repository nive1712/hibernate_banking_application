package com.bank.dao.impl;
import com.bank.model.NetBanking;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NetBankingDaoImpl{
	 
    private SessionFactory sessionFactory; 
	 
	@Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

   
    @Transactional
    public void saveNetBanking(NetBanking netBanking) {
        if (netBanking == null) {
            throw new IllegalArgumentException("NetBanking cannot be null");
        }
        try (Session session = sessionFactory.openSession()) {
            session.save(netBanking);
        }
    }
    @Transactional
    public NetBanking getNetBankingByAccountId(int accountId) {
        return sessionFactory.getCurrentSession().createQuery("FROM NetBanking WHERE user.userId = (SELECT user.userId FROM BankAccount WHERE accountId = :accountId)", NetBanking.class)
                .setParameter("accountId", accountId)
                .uniqueResult();
    }

    @Transactional
    public NetBanking getNetBankingByUserId(int userId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM NetBanking WHERE userId = :userId", NetBanking.class)
                    .setParameter("userId", userId)
                    .uniqueResult();
        }
    }
}



