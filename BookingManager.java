package org.example;

public abstract class PaymentFramework {

    protected double amount;
    protected double discountRate;
    protected double taxRate = 0.12;

    public abstract boolean validatePayment();
    public abstract void finalizeTransaction();

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
        System.out.printf("Original Amount : %.2f%n", this.amount);
        System.out.printf("Discount Rate   : %.0f%%%n", this.discountRate * 100);
        System.out.printf("Discounted Amount: %.2f%n", discountedAmount);
        System.out.printf("VAT (%.0f%%)      : %.2f%n", taxRate * 100, vat);
        System.out.printf("Total           : %.2f%n", total);
        finalizeTransaction();
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getDiscountRate() { return discountRate; }
    public void setDiscountRate(double discountRate) { this.discountRate = discountRate; }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }
}
