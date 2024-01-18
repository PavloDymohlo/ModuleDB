package ua.dymohlo.moduledb;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class OperationDao implements Dao<Operation> {

    private final static OperationDao INSTANCE = new OperationDao();

    public OperationDao() {
    }

    public static OperationDao getInstance() {
        return INSTANCE;
    }

    @Override
    public Operation create(Operation operation) {
        try (Session session = ConnectorManager.openSession()) {
            Transaction transaction = session.getTransaction();
            transaction.begin();
            Account account = operation.getAccount();
            BigDecimal amount = operation.getAmount();
            try {
                if (operation.getType() == OperationType.INCOME) {
                    account.setBalance(account.getBalance().add(amount));
                } else if (operation.getType() == OperationType.EXPENSE) {
                    if (account.getBalance().compareTo(amount) <= 0) {
                        throw new NegativeValueException("The balance of this account does not have enough funds for this operation");
                    }
                    account.setBalance(account.getBalance().subtract(amount));
                }
            } catch (NegativeValueException e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
            session.update(account);
            session.persist(operation);
            transaction.commit();
        }
        return operation;
    }

    @Override
    public Optional<Operation> getById(Long id) {
        try (Session session = ConnectorManager.openSession()) {
            Operation operation = session.find(Operation.class, id);
            if (operation == null) {
                System.out.println("The operation under ID" + id + " does not exist.");
            }
            return Optional.ofNullable(operation);
        }
    }

    @Override
    public Optional<Operation> update(Operation operation) {
        try (Session session = ConnectorManager.openSession()) {
            Transaction transaction = session.getTransaction();
            transaction.begin();
            Operation updateOperation = session.get(Operation.class, operation.getId());
            if (updateOperation == null) {
                throw new RuntimeException("The operation under such ID does not exist.");
            }
            Account account = updateOperation.getAccount();
            BigDecimal amount = updateOperation.getAmount();
            BigDecimal newAmount = operation.getAmount();
            if (updateOperation.getType() == OperationType.INCOME) {
                try {
                    account.setBalance(account.getBalance().subtract(amount));
                } catch (NegativeValueException e) {
                    throw new RuntimeException(e);
                }
            } else if (updateOperation.getType() == OperationType.EXPENSE) {
                try {
                    account.setBalance(account.getBalance().add(amount));
                } catch (NegativeValueException e) {
                    throw new RuntimeException(e);
                }
            }
            updateOperation.setAmount(newAmount);
            updateOperation.setType(operation.getType());
            updateOperation.setCategory(operation.getCategory());
            transaction.commit();
            return Optional.of(updateOperation);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Session session = ConnectorManager.openSession()) {
            Transaction transaction = session.getTransaction();
            transaction.begin();
            Operation operation = session.find(Operation.class, id);
            if (operation != null) {
                System.out.println("Operation's details before deletion: ");
                System.out.println(operation);
                session.remove(operation);
                System.out.println("Operation with id " + id + " was deleted.");
            } else {
                System.out.println("The operation under such ID does not exist.");
            }
            transaction.commit();
        }
    }

    @Override
    public List<Operation> getAll() {
        try (Session session = ConnectorManager.openSession()) {
            return session.createQuery("FROM Operation", Operation.class).list();
        }
    }

    public List<Operation> operationsByCategory(String category) {
        try (Session session = ConnectorManager.openSession()) {
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<Operation> query = criteriaBuilder.createQuery(Operation.class);
            Root<Operation> root = query.from(Operation.class);
            query.select(root).where(criteriaBuilder.equal(root.get("category"), category));
            return session.createQuery(query).getResultList();
        }
    }

    public List<Operation> operationByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        try (Session session = ConnectorManager.openSession()) {
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<Operation> query = criteriaBuilder.createQuery(Operation.class);
            Root<Operation> root = query.from(Operation.class);
            query.select(root).where(criteriaBuilder.between(root.get("transactionTime"), startDate, endDate));
            return session.createQuery(query).getResultList();
        }
    }

    public List<Operation> operationWithMaxAmount() {
        try (Session session = ConnectorManager.openSession()) {
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<Operation> query = criteriaBuilder.createQuery(Operation.class);
            Root<Operation> root = query.from(Operation.class);
            query.select(root).orderBy(criteriaBuilder.desc(root.get("amount")));
            return session.createQuery(query).getResultList();
        }
    }

    public List<Operation> operationWithMinAmount() {
        try (Session session = ConnectorManager.openSession()) {
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<Operation> query = criteriaBuilder.createQuery(Operation.class);
            Root<Operation> root = query.from(Operation.class);
            query.select(root).orderBy(criteriaBuilder.asc(root.get("amount")));
            return session.createQuery(query).getResultList();
        }
    }

    public boolean transferAmounts(Account currentAccount, Account targetAccount, BigDecimal amount) {
        try (Session session = ConnectorManager.openSession()) {
            Transaction transaction = session.getTransaction();
            transaction.begin();
            if (currentAccount.getBalance().compareTo(amount) < 0) {
                System.out.println("The balance of this account does not have enough amount for this operation");
                transaction.rollback();
                return false;
            }
            Operation outputOperation = new Operation();
            outputOperation.setAccount(currentAccount);
            outputOperation.setAmount(amount);
            outputOperation.setType(OperationType.EXPENSE);
            outputOperation.setCategory("Transfer to " + targetAccount.getAccountName());
            outputOperation.setTransactionTime(LocalDateTime.now());

            Operation inputOperation = new Operation();
            inputOperation.setAccount(targetAccount);
            inputOperation.setAmount(amount);
            inputOperation.setType(OperationType.INCOME);
            inputOperation.setCategory("Transfer from " + currentAccount.getAccountName());
            inputOperation.setTransactionTime(LocalDateTime.now());

            currentAccount.setBalance(currentAccount.getBalance().subtract(amount));
            targetAccount.setBalance(targetAccount.getBalance().add(amount));

            session.persist(outputOperation);
            session.persist(inputOperation);
            session.update(currentAccount);
            session.update(targetAccount);

            transaction.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Operation> findOperationsByUserAndPeriod(User user, LocalDateTime startDate, LocalDateTime endDate) {
        try (Session session = ConnectorManager.openSession()) {
            String hql = "FROM Operation operation WHERE operation.account.user = :user AND operation.transactionTime BETWEEN :startDate AND :endDate";
            Query<Operation> query = session.createQuery(hql, Operation.class);
            query.setParameter("user", user);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            List<Operation> operations = query.getResultList();

            if (operations.isEmpty()) {
                System.out.println("No transactions were found for this period.");
                return Collections.emptyList();
            }

            Map<User, List<Operation>> userOperationsMap = new HashMap<>();
            for (Operation operation : operations) {
                Hibernate.initialize(operation.getAccount().getUser());
                User findUser = operation.getAccount().getUser();

                List<Operation> userOperations = userOperationsMap.get(findUser);
                if (userOperations == null) {
                    userOperations = new ArrayList<>();
                    userOperationsMap.put(findUser, userOperations);
                }
                userOperations.add(operation);
            }
            for (Map.Entry<User, List<Operation>> entry : userOperationsMap.entrySet()) {
                User currentUser = entry.getKey();
                System.out.println("User: " + currentUser.getFullName());

                List<Operation> userOperations = entry.getValue();
                Account userAccount = userOperations.get(0).getAccount();
                System.out.println("Account: " + userAccount.getAccountName());
                for (Operation operation : userOperations) {
                    System.out.println(operation);
                }
                System.out.println("End of list of operations.");
                System.out.println("---------------------------------");
            }
            return operations;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
