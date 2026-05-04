package org.example;

public abstract class PaymentFramework {

    protected double amount;
    protected double discountRate;
    protected double taxRate = 0.12;
    protected boolean successful = false;

    // ─────────────────────────────────────────
    // ABSTRACT METHODS
    // ─────────────────────────────────────────

    public abstract boolean validatePayment();
    public abstract void finalizeTransaction();

    // ─────────────────────────────────────────
    // CONCRETE METHODS
    // ─────────────────────────────────────────

    public double calculateVAT(double amount) {
        return amount * taxRate;
    }

    public double applyDiscount(double amount, double discountRate) {
        return amount - (amount * discountRate);
    }

    public void processInvoice() {
        if (!validatePayment()) {
            System.out.println("Payment validation failed. Invoice cannot be processed.");
            return;
        }

        double discountedAmount = applyDiscount(this.amount, this.discountRate);
        double vat = calculateVAT(discountedAmount);
        double total = discountedAmount + vat;

        System.out.println("=== Invoice ===");
        System.out.printf("Original Amount    : %.2f%n", this.amount);
        System.out.printf("Discount Rate      : %.0f%%%n", this.discountRate * 100);
        System.out.printf("Discounted Amount  : %.2f%n", discountedAmount);
        System.out.printf("VAT (%.0f%%)          : %.2f%n", taxRate * 100, vat);
        System.out.printf("Total              : %.2f%n", total);

        finalizeTransaction();
    }

    // Returns the final payable amount (after discount + VAT)
    public double getTotalPayable() {
        double discountedAmount = applyDiscount(this.amount, this.discountRate);
        double vat = calculateVAT(discountedAmount);
        return discountedAmount + vat;
    }

    // ─────────────────────────────────────────
    // FLAG: was payment successful?
    // ─────────────────────────────────────────

    public boolean isSuccessful() {
        return successful;
    }

    // ─────────────────────────────────────────
    // GETTERS & SETTERS
    // ─────────────────────────────────────────

    public double getAmount()                         { return amount; }
    public void   setAmount(double amount)            { this.amount = amount; }

    public double getDiscountRate()                   { return discountRate; }
    public void   setDiscountRate(double discountRate){ this.discountRate = discountRate; }

    public double getTaxRate()                        { return taxRate; }
    public void   setTaxRate(double taxRate)          { this.taxRate = taxRate; }
}

class CashPayment extends PaymentFramework {

    private final double cashTendered;

    public CashPayment(double amount, double discountRate, double cashTendered) {
        this.amount        = amount;
        this.discountRate  = discountRate;
        this.cashTendered  = cashTendered;
    }

    // ─────────────────────────────────────────
    // ABSTRACT METHOD IMPLEMENTATIONS
    // ─────────────────────────────────────────

    @Override
    public boolean validatePayment() {
        double totalPayable = getTotalPayable();
        if (amount <= 0) {
            System.out.println("Invalid amount. Must be greater than 0.");
            return false;
        }
        if (cashTendered < totalPayable) {
            System.out.printf("Insufficient cash. Total payable is PHP %.2f but you provided PHP %.2f.%n",
                    totalPayable, cashTendered);
            return false;
        }
        return true;
    }

    @Override
    public void finalizeTransaction() {
        System.out.println("Cash payment processed successfully.");
        successful = true;
    }

    // ─────────────────────────────────────────
    // OVERRIDE processInvoice for cash display
    // ─────────────────────────────────────────

    @Override
    public void processInvoice() {
        if (!validatePayment()) {
            System.out.println("Payment validation failed. Invoice cannot be processed.");
            return;
        }

        double discountedAmount = applyDiscount(this.amount, this.discountRate);
        double vat              = calculateVAT(discountedAmount);
        double total            = discountedAmount + vat;
        double change           = cashTendered - total;

        System.out.println("\n=== Cash Invoice ===");
        System.out.printf("Original Amount    : PHP %.2f%n", this.amount);
        System.out.printf("Discount Rate      : %.0f%%%n",   this.discountRate * 100);
        System.out.printf("Discounted Amount  : PHP %.2f%n", discountedAmount);
        System.out.printf("VAT (12%%)          : PHP %.2f%n", vat);
        System.out.printf("Total Payable      : PHP %.2f%n", total);
        System.out.printf("Cash Tendered      : PHP %.2f%n", cashTendered);
        System.out.printf("Change             : PHP %.2f%n", change);

        finalizeTransaction();
    }
}

class CreditCardPayment extends PaymentFramework {

    private final double creditLimit;

    public CreditCardPayment(double amount, double discountRate, double creditLimit) {
        this.amount       = amount;
        this.discountRate = discountRate;
        this.creditLimit  = creditLimit;
    }

    // ─────────────────────────────────────────
    // ABSTRACT METHOD IMPLEMENTATIONS
    // ─────────────────────────────────────────

    @Override
    public boolean validatePayment() {
        double totalPayable = getTotalPayable();
        if (amount <= 0) {
            System.out.println("Invalid amount. Must be greater than 0.");
            return false;
        }
        if (totalPayable > creditLimit) {
            System.out.printf("Credit limit exceeded. Total payable PHP %.2f exceeds your limit of PHP %.2f.%n",
                    totalPayable, creditLimit);
            return false;
        }
        return true;
    }

    @Override
    public void finalizeTransaction() {
        System.out.println("Credit card payment processed successfully.");
        successful = true;
    }

    // ─────────────────────────────────────────
    // OVERRIDE processInvoice for credit-card display
    // ─────────────────────────────────────────

    @Override
    public void processInvoice() {
        if (!validatePayment()) {
            System.out.println("Payment validation failed. Invoice cannot be processed.");
            return;
        }

        double discountedAmount = applyDiscount(this.amount, this.discountRate);
        double vat              = calculateVAT(discountedAmount);
        double total            = discountedAmount + vat;

        System.out.println("\n=== Credit Card Invoice ===");
        System.out.printf("Original Amount    : PHP %.2f%n", this.amount);
        System.out.printf("Discount Rate      : %.0f%%%n",   this.discountRate * 100);
        System.out.printf("Discounted Amount  : PHP %.2f%n", discountedAmount);
        System.out.printf("VAT (12%%)          : PHP %.2f%n", vat);
        System.out.printf("Total Payable      : PHP %.2f%n", total);
        System.out.printf("Credit Limit       : PHP %.2f%n", creditLimit);
        System.out.printf("Remaining Limit    : PHP %.2f%n", creditLimit - total);

        finalizeTransaction();
    }
}
