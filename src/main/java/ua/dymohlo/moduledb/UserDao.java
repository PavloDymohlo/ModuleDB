package ua.dymohlo.moduledb;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class UserDao implements Dao<User> {

    private final static UserDao INSTANCE = new UserDao();

    public UserDao() {
    }

    public static UserDao getInstance() {
        return INSTANCE;
    }

    @Override
    public User create(User user) {
        try (Session session = ConnectorManager.openSession()) {
            Transaction transaction = session.getTransaction();
            transaction.begin();
            session.persist(user);
            transaction.commit();
        }
        return user;
    }

    @Override
    public Optional<User> getById(Long id) {
        try (Session session = ConnectorManager.openSession()) {
            User user = session.find(User.class, id);
            if (user == null) {
                System.out.println("The user under such ID does not exist.");
            }
            return Optional.ofNullable(user);
        }
    }

    @Override
    public Optional<User> update(User user) {
        try (Session session = ConnectorManager.openSession()) {
            Transaction transaction = session.getTransaction();
            transaction.begin();
            User updateUser = session.get(User.class, user.getId());
            updateUser.setFullName(user.getFullName());
            session.merge(updateUser);
            transaction.commit();
            return Optional.of(updateUser);
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Session session = ConnectorManager.openSession()) {
            Transaction transaction = session.getTransaction();
            transaction.begin();
            User user = session.find(User.class, id);
            if (user != null) {
                System.out.println("User's details before deletion: ");
                System.out.println(user);
                session.remove(user);
                System.out.println("User with id " + id + " was deleted.");
            } else {
                System.out.println("The user under such ID does not exist.");
            }
            transaction.commit();
        }
    }

    @Override
    public List<User> getAll() {
        try (Session session = ConnectorManager.openSession()) {
            return session.createQuery("SELECT user FROM User user LEFT JOIN FETCH user.accounts", User.class).list();
        }
    }

    public void userAccounts(Long userId) {
        try (Session session = ConnectorManager.openSession()) {
            if (userId != null) {
                User user = session.get(User.class, userId);
                if (user != null) {
                    System.out.print(user + " has ");
                    List<Account> accounts = user.getAccounts();
                    if (accounts.isEmpty()) {
                        System.out.println(" no accounts.");
                    } else {
                        for (Account account : accounts) {
                            System.out.println(account);
                        }
                    }
                } else {
                    System.out.println("The user under such ID does not exist.");
                }
            }
        }
    }
}