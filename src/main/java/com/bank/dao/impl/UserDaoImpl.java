package com.bank.dao.impl;

import com.bank.model.User;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserDaoImpl {
	 private static final String ERROR_MESSAGE = "An error occurred while updating the bank account.";
	private static final Logger logger = LogManager.getLogger(UserDaoImpl.class);
	
	private SessionFactory sessionFactory; 
	
	@Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    @Transactional
    public int saveUser(User user) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.save(user);
            transaction.commit();
            return user.getUserId();
        } catch (Exception e) {
        	logger.error( ERROR_MESSAGE, e);
            return -1;
        }
    }

    @Transactional
    public User findUserByEmail(String email) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM User WHERE email = :email", User.class)
                    .setParameter("email", email)
                    .uniqueResult();
        } catch (Exception e) {
        	logger.error( ERROR_MESSAGE, e);
            return null;
        }
    }

  
    @Transactional
    public User getUserByEmail(String email) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM User WHERE email = :email", User.class)
                    .setParameter("email", email)
                    .uniqueResult();
        } catch (Exception e) {
        	logger.error( ERROR_MESSAGE, e);
            return null;
        }
    }
    
    @Transactional
    public User getUserById(int userId) {
        return sessionFactory.getCurrentSession().get(User.class, userId);
      
    }  
 


}
