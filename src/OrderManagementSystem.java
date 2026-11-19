import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class OrderManagementSystem {

    private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root123"; // Replace with your MySQL root password

    private static final List<String> VALID_STATUSES = Arrays.asList("Pending", "Shipped", "Delivered", "Cancelled");

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Scanner sc = new Scanner(System.in)) {

            System.out.println("Connected to ecommerce_db successfully.");
            int choice;

            do {
                System.out.println("\n--- E-Commerce Order Management System ---");
                System.out.println("1. Insert Order Records (at least 3)");
                System.out.println("2. Display Orders by Status (Quantity > 2, Sorted by Unit Price DESC)");
                System.out.println("3. Increase Order Quantity");
                System.out.println("4. Update Order Status");
                System.out.println("5. Display Total Order Value & Delete Cancelled Order");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");
                choice = sc.nextInt();
                sc.nextLine(); // consume newline

                switch (choice) {
                    case 1 -> insertOrders(conn, sc);
                    case 2 -> displayFilteredOrders(conn, sc);
                    case 3 -> increaseQuantity(conn, sc);
                    case 4 -> updateOrderStatus(conn, sc);
                    case 5 -> totalValueAndDelete(conn, sc);
                    case 6 -> System.out.println("Exiting application...");
                    default -> System.out.println("Invalid choice. Try again.");
                }
            } while (choice != 6);

        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    // Q1 (a): Insert at least three order records and handle duplicate order IDs
    private static void insertOrders(Connection conn, Scanner sc) {
        System.out.print("How many orders do you want to insert (minimum 3)? ");
        int count = sc.nextInt();
        sc.nextLine();

        String sql = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                System.out.println("\nEnter details for Order #" + i + ":");
                System.out.print("Order ID: ");
                int orderId = sc.nextInt();
                sc.nextLine();

                System.out.print("Customer Name: ");
                String customerName = sc.nextLine();

                System.out.print("Product Name: ");
                String productName = sc.nextLine();

                System.out.print("Quantity: ");
                int quantity = sc.nextInt();

                System.out.print("Unit Price: ");
                double unitPrice = sc.nextDouble();
                sc.nextLine();

                System.out.print("Status (Pending/Shipped/Delivered/Cancelled): ");
                String status = sc.nextLine().trim();

                pstmt.setInt(1, orderId);
                pstmt.setString(2, customerName);
                pstmt.setString(3, productName);
                pstmt.setInt(4, quantity);
                pstmt.setDouble(5, unitPrice);
                pstmt.setString(6, status);

                try {
                    pstmt.executeUpdate();
                    System.out.println("Order ID " + orderId + " inserted successfully.");
                } catch (SQLIntegrityConstraintViolationException dupEx) {
                    System.out.println("Error: Duplicate Order ID (" + orderId + "). Record skipped.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error inserting records: " + e.getMessage());
        }
    }

    // Q1 (b): Display orders by status with quantity > 2 sorted by unit_price DESC
    private static void displayFilteredOrders(Connection conn, Scanner sc) {
        System.out.print("Enter order status to filter: ");
        String status = sc.nextLine().trim();

        String sql = "SELECT * FROM Orders WHERE LOWER(status) = LOWER(?) AND quantity > 2 ORDER BY unit_price DESC";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            boolean found = false;
            System.out.println("\n--- Matching Orders ---");
            while (rs.next()) {
                found = true;
                printOrderRow(rs);
            }
            if (!found) {
                System.out.println("No orders found with status '" + status + "' and quantity > 2.");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching orders: " + e.getMessage());
        }
    }

    // Q1 (c): Increase existing quantity of a specified order
    private static void increaseQuantity(Connection conn, Scanner sc) {
        System.out.print("Enter Order ID: ");
        int orderId = sc.nextInt();
        System.out.print("Enter additional quantity to add: ");
        int addQty = sc.nextInt();
        sc.nextLine();

        String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, addQty);
            pstmt.setInt(2, orderId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Quantity updated successfully for Order ID " + orderId + ".");
            } else {
                System.out.println("Order ID " + orderId + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Error updating quantity: " + e.getMessage());
        }
    }

    // Q1 (d): Validate status, update order, and display updated details
    private static void updateOrderStatus(Connection conn, Scanner sc) {
        System.out.print("Enter Order ID: ");
        int orderId = sc.nextInt();
        sc.nextLine();

        String selectSql = "SELECT * FROM Orders WHERE order_id = ?";
        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setInt(1, orderId);
            ResultSet rs = selectStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("Order ID " + orderId + " does not exist.");
                return;
            }

            System.out.println("Current Order Details:");
            printOrderRow(rs);

            System.out.print("Enter new status (Pending/Shipped/Delivered/Cancelled): ");
            String newStatus = sc.nextLine().trim();

            String matchedStatus = VALID_STATUSES.stream()
                    .filter(s -> s.equalsIgnoreCase(newStatus))
                    .findFirst()
                    .orElse(null);

            if (matchedStatus == null) {
                System.out.println("Invalid status. Allowed values: Pending, Shipped, Delivered, Cancelled.");
                return;
            }

            String updateSql = "UPDATE Orders SET status = ? WHERE order_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, matchedStatus);
                updateStmt.setInt(2, orderId);
                updateStmt.executeUpdate();
            }

            System.out.println("Status updated successfully. Updated Order Details:");
            ResultSet updatedRs = selectStmt.executeQuery();
            if (updatedRs.next()) {
                printOrderRow(updatedRs);
            }
        } catch (SQLException e) {
            System.out.println("Error updating status: " + e.getMessage());
        }
    }

    // Q1 (e): Display total order value and delete order only if status is Cancelled
    private static void totalValueAndDelete(Connection conn, Scanner sc) {
        String sumSql = "SELECT SUM(quantity * unit_price) AS total_value FROM Orders";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sumSql)) {
            if (rs.next()) {
                double totalValue = rs.getDouble("total_value");
                System.out.printf("Total Order Value across all orders: %.2f%n", totalValue);
            }
        } catch (SQLException e) {
            System.out.println("Error calculating total order value: " + e.getMessage());
        }

        System.out.print("Enter Order ID to delete: ");
        int orderId = sc.nextInt();
        sc.nextLine();

        String checkSql = "SELECT status FROM Orders WHERE order_id = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, orderId);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("Order ID " + orderId + " does not exist.");
                return;
            }

            String status = rs.getString("status");
            if ("Cancelled".equalsIgnoreCase(status)) {
                String deleteSql = "DELETE FROM Orders WHERE order_id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, orderId);
                    deleteStmt.executeUpdate();
                    System.out.println("Cancelled Order ID " + orderId + " deleted successfully.");
                }
            } else {
                System.out.println("Cannot delete Order ID " + orderId + " because its status is '" + status + "' (only 'Cancelled' orders can be deleted).");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting order: " + e.getMessage());
        }
    }

    private static void printOrderRow(ResultSet rs) throws SQLException {
        System.out.printf("ID: %d | Customer: %s | Product: %s | Qty: %d | Unit Price: %.2f | Status: %s%n",
                rs.getInt("order_id"),
                rs.getString("customer_name"),
                rs.getString("product_name"),
                rs.getInt("quantity"),
                rs.getDouble("unit_price"),
                rs.getString("status"));
    }
}
