package ua.dymohlo.moduledb;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main {

    static final Scanner scan = new Scanner(System.in);

    public static void main(String[] args) {

        UserDao userDao = UserDao.getInstance();
        AccountDao accountDao = AccountDao.getInstance();
        OperationDao operationDao = OperationDao.getInstance();
        ReportBuilder reportBuilder = new ReportBuilder();

        createFile();
        createUser(userDao);
        System.out.println(userDao.getById(2L));
        updateUser(userDao);
        userDao.deleteById(7L);
        System.out.println(userDao.getAll());
        userDao.userAccounts(1L);

        createAccount(accountDao, userDao);
        System.out.println(accountDao.getById(7L));
        updateAccount(accountDao, userDao);
        accountDao.deleteById(8L);
        System.out.println(accountDao.getAll());

        createOperation(operationDao, accountDao);
        System.out.println(operationDao.getById(3L));
        updateOperation(operationDao);
        operationDao.deleteById(16L);
        System.out.println(operationDao.getAll());
        System.out.println(operationDao.operationsByCategory("premium"));
        operationByPeriod(operationDao);
        System.out.println(operationDao.operationWithMaxAmount());
        System.out.println(operationDao.operationWithMinAmount());
        transferAmount(operationDao, accountDao);
        findOperationsByAccountAndPeriod(operationDao, userDao);
        buildReport(userDao);
    }

    private static void createFile() {
        File operationsReport = new File("C:\\Users\\DELL\\IdeaProjects\\ModuleDB\\src\\main\\java\\ua\\dymohlo\\moduledb\\operationsReport.csv");
        try {
            operationsReport.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createUser(UserDao userDao) {
        User user = new User();
        user.setFullName("Jimmy");
        userDao.create(user);
        System.out.println(user);
    }

    private static void updateUser(UserDao userDao) {
        Optional<User> getUser = userDao.getById(8L);
        if (getUser.isPresent()) {
            User updateUser = getUser.get();
            updateUser.setFullName("T-1000");
            Optional<User> update = userDao.update(updateUser);
            System.out.println(update);
        }
    }

    private static void createAccount(AccountDao accountDao, UserDao userDao) {
        Account account = new Account();
        Optional<User> getUser = userDao.getById(6L);
        if (getUser.isEmpty()) {
            return;
        }
        User accountUser = getUser.get();
        account.setAccountName("Salary card");
        boolean validInputBalance = false;
        while (!validInputBalance) {
            try {
                System.out.println("Enter balance: ");
                BigDecimal balance = scan.nextBigDecimal();
                account.setBalance(balance);
                validInputBalance = true;
            } catch (NegativeValueException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
        account.setUser(accountUser);
        accountDao.create(account);
        System.out.println(account);
    }

    private static void updateAccount(AccountDao accountDao, UserDao userDao) {
        Optional<Account> getAccount = accountDao.getById(7L);
        if (getAccount.isPresent()) {
            Account updateAccount = getAccount.get();
            Optional<User> newUser = userDao.getById(Long.valueOf(2L));
            if (newUser.isPresent()) {
                User user = newUser.get();
                updateAccount.setAccountName("Premium card");
                updateAccount.setUser(user);
                accountDao.update(updateAccount);
                System.out.println(user + " " + getAccount);
            }
        }
    }

    private static void createOperation(OperationDao operationDao, AccountDao accountDao) {
        Operation operation = new Operation();
        Optional<Account> getAccount = accountDao.getById(9L);
        if (getAccount.isEmpty()) {
            return;
        }
        Account account = getAccount.get();
        boolean validInputAmount = false;
        while (!validInputAmount) {
            try {
                System.out.println("Enter amount: ");
                BigDecimal amount = scan.nextBigDecimal();
                operation.setAmount(amount);
                validInputAmount = true;
            } catch (NegativeValueException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
        operation.setType(OperationType.EXPENSE);
        operation.setCategory("cinema");
        operation.setTransactionTime(LocalDateTime.now());
        operation.setAccount(account);
        operationDao.create(operation);

        System.out.println(operation);
    }

    public static void updateOperation(OperationDao operationDao) {
        Optional<Operation> getOperation = operationDao.getById(13L);
        if (getOperation.isPresent()) {
            Operation operation = getOperation.get();
            try {
                BigDecimal newOperationAmount = BigDecimal.valueOf(100);
                operation.setAmount(new BigDecimal(String.valueOf(newOperationAmount)));
            } catch (NegativeValueException e) {
                throw new RuntimeException(e);
            }
            operation.setCategory("premium");
            operation.setType(OperationType.INCOME);
            operationDao.update(operation);
        }
    }

    private static void operationByPeriod(OperationDao operationDao) {
        LocalDateTime startDate = LocalDateTime.of(2024, Month.JANUARY, 14, 00, 00, 00);
        LocalDateTime endEnd = LocalDateTime.of(2024, Month.JANUARY, 19, 00, 00, 00);
        List<Operation> operations = operationDao.operationByPeriod(startDate, endEnd);
        System.out.println(operations);
    }

    private static void transferAmount(OperationDao operationDao, AccountDao accountDao) {
        Optional<Account> currentAccountDAO = accountDao.getById(7L);
        if (currentAccountDAO.isPresent()) {
            Account currentAccount = currentAccountDAO.get();
            Optional<Account> targetAccountDAO = accountDao.getById(6L);
            if (targetAccountDAO.isPresent()) {
                Account targetAccount = targetAccountDAO.get();
                BigDecimal transactionAmount = new BigDecimal(8123);
                boolean transferSuccessful = operationDao.transferAmounts(currentAccount, targetAccount, transactionAmount);
                if (transferSuccessful) {
                    System.out.println("Amount transferred successfully.");
                }
            }
        }
    }

    private static void findOperationsByAccountAndPeriod(OperationDao operationDao, UserDao userDao) {
        Optional<User> getUser = userDao.getById(6L);
        if (getUser.isEmpty()) {
            return;
        }
        User user = getUser.get();
        LocalDateTime startDate = LocalDateTime.of(2024, Month.JANUARY, 14, 00, 00, 00);
        LocalDateTime endDate = LocalDateTime.of(2024, Month.JANUARY, 20, 00, 00, 00);
        operationDao.findOperationsByUserAndPeriod(user, startDate, endDate);
    }

    private static void buildReport(UserDao userDao) {
        ReportBuilder reportBuilder = new ReportBuilder();
        Optional<User> getUser = userDao.getById(6L);
        if (getUser.isEmpty()) {
            return;
        }
        User user = getUser.get();
        LocalDateTime startDate = LocalDateTime.of(2024, Month.JANUARY, 14, 00, 00, 00);
        LocalDateTime endDate = LocalDateTime.of(2024, Month.JANUARY, 20, 00, 00, 00);
        String operationsReport = "C:\\Users\\DELL\\IdeaProjects\\ModuleDB\\src\\main\\java\\ua\\dymohlo\\moduledb\\operationsReport.csv";
        reportBuilder.buildReport(user, startDate, endDate, operationsReport);
    }

    private static String input(String message) {
        Scanner scan = new Scanner(System.in);
        System.out.println(message);
        return scan.nextLine();
    }
}
