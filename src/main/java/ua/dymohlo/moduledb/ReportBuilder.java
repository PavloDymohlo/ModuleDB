package ua.dymohlo.moduledb;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ReportBuilder {
    public List<Operation> buildReport(User user, LocalDateTime startDate, LocalDateTime endDate, String filePath) {
        try {
            OperationDao operationDao = OperationDao.getInstance();
            List<Operation> operations = operationDao.findOperationsByUserAndPeriod(user, startDate, endDate);

            if (operations.isEmpty()) {
                System.out.println("No transactions were found for this period.");
                return Collections.emptyList();
            }

            try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filePath, true))) {
                if (Files.size(Paths.get(filePath)) == 0) {
                    csvWriter.writeNext(new String[]{"User", "Account", "Operation ID", "Type", "Amount", "Category", "Transaction Time"});
                }
                csvWriter.writeNext(new String[]{});

                for (Operation operation : operations) {
                    String[] rowData = {
                            operation.getAccount().getUser().getFullName(),
                            operation.getAccount().getAccountName(),
                            String.valueOf(operation.getId()),
                            operation.getType().toString(),
                            operation.getAmount().toString(),
                            operation.getCategory(),
                            operation.getTransactionTime().toString(),
                    };
                    csvWriter.writeNext(rowData);
                }
                csvWriter.writeNext(new String[]{});
                System.out.println("Report for user " + user.getFullName() + " created or updated successfully at: " + filePath);
            } catch (IOException e) {
                throw new RuntimeException("Error creating or updating report: " + e.getMessage());
            }
            return operations;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}


