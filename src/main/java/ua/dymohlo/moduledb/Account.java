package ua.dymohlo.moduledb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
@Table(schema = "user_accounts", name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    @Column(name = "account_name", nullable = false)
    private String accountName;
    @Column(name = "balance", nullable = false)
    private BigDecimal balance;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public void setUser(User user) {
        this.user = user;
    }

    public void setBalance(BigDecimal balance) throws NegativeValueException {
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeValueException("The balance cannot be negative.");
        }
        this.balance = balance;
    }
}
