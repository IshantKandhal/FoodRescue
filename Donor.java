import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class Donor {

    Scanner sc;
    Donor(Scanner sc) {this.sc = sc;}

    // ================= DONOR MENU =================

    void donorMenu(String donorName) {

        int choice;

        do {

            System.out.println("\n========== DONOR MENU ==========");
            System.out.println("1. Donate Food");
            System.out.println("2. View My Donations");
            System.out.println("3. Logout");

            System.out.print("Enter choice : ");

            try {
                choice = sc.nextInt();
                sc.nextLine();
            }
            catch(Exception e) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine();
                choice = 0;
                continue;
            }

            switch(choice) {

                case 1:
                    donateFood(donorName);
                    break;

                case 2:
                    viewMyDonations(donorName);
                    break;

                case 3:
                    System.out.println("Donor logged out successfully.");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while(choice != 3);
    }


    // ================= DONATE FOOD =================

    void donateFood(String donorName) {

        System.out.println("\n---------- DONATE FOOD ----------");

        System.out.print("Enter food name : ");
        String foodName = sc.nextLine();

        if(foodName.trim().isEmpty()) {
            System.out.println("Food name cannot be empty.");
            return;
        }

        int quantity;

        System.out.print("Enter quantity : ");

        try {
            quantity = sc.nextInt();
            sc.nextLine();
        }
        catch(Exception e) {
            System.out.println("Invalid quantity. Please enter a number.");
            sc.nextLine();
            return;
        }

        if(quantity <= 0) {
            System.out.println("Quantity must be greater than 0.");
            return;
        }

        String expiryDate;

        System.out.print("Enter expiry date (YYYY-MM-DD) : ");
        expiryDate = sc.nextLine();

        if(expiryDate.trim().isEmpty()) {
            System.out.println("Expiry date cannot be empty.");
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;

        try {

            con = DBconnection.getConnection();

            String query =
                "INSERT INTO donations (donor_name, food_name, quantity, expiry_time, status) " +
                "VALUES (?, ?, ?, ?, 'Available')";

            ps = con.prepareStatement(query);

            ps.setString(1, donorName);
            ps.setString(2, foodName);
            ps.setInt(3, quantity);
            ps.setString(4, expiryDate);

            ps.executeUpdate();

            System.out.println("\nDonation added successfully!");
            System.out.println("Food     : " + foodName);
            System.out.println("Quantity : " + quantity);
            System.out.println("Expires  : " + expiryDate);

        }
        catch(SQLException e) {

            System.out.println("Exception : " + e.getMessage());
        }
        finally {

            try {
                if(ps != null) ps.close();
                if(con != null) con.close();
            }
            catch(SQLException e) {
                System.out.println("Closing Exception : " + e.getMessage());
            }
        }
    }


    // ================= VIEW MY DONATIONS =================

    void viewMyDonations(String donorName) {

        System.out.println("\n---------- MY DONATIONS ----------");

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            con = DBconnection.getConnection();

            String query =
                "SELECT * FROM donations WHERE donor_name = ?";

            ps = con.prepareStatement(query);

            ps.setString(1, donorName);

            rs = ps.executeQuery();

            boolean found = false;

            while(rs.next()) {

                found = true;

                System.out.println("----------------------------");
                System.out.println("Donation ID : " + rs.getInt("id"));
                System.out.println("Food        : " + rs.getString("food_name"));
                System.out.println("Quantity    : " + rs.getInt("quantity"));
                System.out.println("Expiry      : " + rs.getDate("expiry_time"));
                System.out.println("Status      : " + rs.getString("status"));
            }

            if(!found) {
                System.out.println("No donations found.");
            }

        }
        catch(SQLException e) {

            System.out.println("Exception : " + e.getMessage());
        }
        finally {

            try {
                if(rs != null) rs.close();
                if(ps != null) ps.close();
                if(con != null) con.close();
            }
            catch(SQLException e) {
                System.out.println("Closing Exception : " + e.getMessage());
            }
        }
    }
}