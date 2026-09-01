import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class NGO {

    Scanner sc;
    Volunteer volunteer;
    NGO(Scanner sc, Volunteer volunteer) {
        this.sc = sc;
        this.volunteer = volunteer;
    }

    void ngoMenu(String ngoName) {

        int choice;

        do {

            System.out.println("\n========== NGO MENU ==========");
            System.out.println("1. Request Food");
            System.out.println("2. View My Requests");
            System.out.println("3. View Food Allocation");
            System.out.println("4. View Delivery");
            System.out.println("5. Cancel Request");
            System.out.println("6. Logout");

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
                    requestFood(ngoName);
                    break;

                case 2:
                    viewRequests(ngoName);
                    break;

                case 3:
                    viewAllocation(ngoName);
                    break;

                case 4:
                    viewDelivery(ngoName);
                    break;

                case 5:
                    cancelRequest(ngoName);
                    break;

                case 6:
                    System.out.println("NGO logged out successfully.");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while(choice != 6);
    }

    void requestFood(String ngoName) {

        System.out.println("\n---------- REQUEST FOOD ----------");

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


        Connection con = null;
        PreparedStatement requestPs = null;
        PreparedStatement donationPs = null;
        PreparedStatement allocationPs = null;
        PreparedStatement updateDonationPs = null;
        ResultSet rs = null;

        try {

            con = DBconnection.getConnection();
            con.setAutoCommit(false);

            String donationQuery =
                "SELECT id, donor_name, quantity " +
                "FROM donations " +
                "WHERE LOWER(food_name) = LOWER(?) " +
                "AND quantity > 0 " +
                "AND expiry_time >= CURDATE() " +
                "ORDER BY expiry_time";

            donationPs = con.prepareStatement(donationQuery);

            donationPs.setString(1, foodName);

            rs = donationPs.executeQuery();


            if(!rs.next()) {

                System.out.println(
                    "No matching donation is currently available."
                );

                con.rollback();
                return;
            }


            int donationId = rs.getInt("id");
            String donorName = rs.getString("donor_name");
            int availableQuantity = rs.getInt("quantity");


            if(availableQuantity < quantity) {

                System.out.println(
                    "Requested quantity is not available."
                );

                System.out.println(
                    "Available quantity : " + availableQuantity
                );

                con.rollback();
                return;
            }


            String requestQuery =
                "INSERT INTO requests " +
                "(ngo_name, food_name, quantity) " +
                "VALUES (?, ?, ?)";

            requestPs = con.prepareStatement(
                requestQuery,
                java.sql.Statement.RETURN_GENERATED_KEYS
            );

            requestPs.setString(1, ngoName);
            requestPs.setString(2, foodName);
            requestPs.setInt(3, quantity);

            requestPs.executeUpdate();


            ResultSet generatedKeys =
                requestPs.getGeneratedKeys();

            int requestId;

            if(generatedKeys.next()) {

                requestId = generatedKeys.getInt(1);

            }
            else {

                System.out.println("Unable to create request.");

                generatedKeys.close();
                con.rollback();
                return;
            }

            generatedKeys.close();


            String allocationQuery =
                "INSERT INTO allocations " +
                "(request_id, donation_id, quantity_taken) " +
                "VALUES (?, ?, ?)";

            allocationPs = con.prepareStatement(allocationQuery);

            allocationPs.setInt(1, requestId);
            allocationPs.setInt(2, donationId);
            allocationPs.setInt(3, quantity);

            allocationPs.executeUpdate();


            String updateDonationQuery =
                "UPDATE donations " +
                "SET quantity = quantity - ? " +
                "WHERE id = ?";

            updateDonationPs =
                con.prepareStatement(updateDonationQuery);

            updateDonationPs.setInt(1, quantity);
            updateDonationPs.setInt(2, donationId);

            updateDonationPs.executeUpdate();


            con.commit();


            System.out.println("\nFood request submitted successfully.");
            System.out.println("Request ID      : " + requestId);
            System.out.println("Food            : " + foodName);
            System.out.println("Quantity        : " + quantity);
            System.out.println("Matched Donor   : " + donorName);
            System.out.println("Donation ID     : " + donationId);
            System.out.println("Allocation      : Created");
            System.out.println("Status          : Pending");

            // automatically assign a free volunteer to this request
            // (-1 means "no specific logged-in volunteer" -> picks any free one)
            volunteer.assignVolunteer(requestId, donationId, -1);


        }
        catch(SQLException e) {

            System.out.println("Exception : " + e.getMessage());

            if(con != null) {

                try {

                    con.rollback();

                }
                catch(SQLException ex) {

                    System.out.println(
                        "Rollback Exception : " + ex.getMessage()
                    );
                }
            }
        }
        finally {

            try {

                if(rs != null)
                    rs.close();

                if(requestPs != null)
                    requestPs.close();

                if(donationPs != null)
                    donationPs.close();

                if(allocationPs != null)
                    allocationPs.close();

                if(updateDonationPs != null)
                    updateDonationPs.close();

                if(con != null) {

                    con.setAutoCommit(true);
                    con.close();
                }

            }
            catch(SQLException e) {

                System.out.println(
                    "Closing Exception : " + e.getMessage()
                );
            }
        }
    }

    void viewRequests(String ngoName) {

        System.out.println("\n---------- MY REQUESTS ----------");

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            con = DBconnection.getConnection();

            String query =
                "SELECT * FROM requests WHERE ngo_name = ?";

            ps = con.prepareStatement(query);

            ps.setString(1, ngoName);

            rs = ps.executeQuery();

            boolean found = false;

            while(rs.next()) {

                found = true;

                System.out.println("----------------------------");
                System.out.println("Request ID : " + rs.getInt("id"));
                System.out.println("Food       : " + rs.getString("food_name"));
                System.out.println("Quantity   : " + rs.getInt("quantity"));
                System.out.println("Status     : " + rs.getString("status"));
            }

            if(!found) {

                System.out.println("No requests found.");
            }

        }
        catch(SQLException e) {

            System.out.println("Exception : " + e.getMessage());

        }
        finally {

            try {

                if(rs != null)
                    rs.close();

                if(ps != null)
                    ps.close();

                if(con != null)
                    con.close();

            }
            catch(SQLException e) {

                System.out.println(
                    "Closing Exception : " + e.getMessage()
                );
            }
        }
    }

    void viewAllocation(String ngoName) {

        System.out.println("\n---------- FOOD ALLOCATION ----------");

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            con = DBconnection.getConnection();

            String query =
                "SELECT a.id, r.food_name, a.quantity_taken, d.donor_name " +
                "FROM allocations a " +
                "JOIN requests r ON a.request_id = r.id " +
                "JOIN donations d ON a.donation_id = d.id " +
                "WHERE r.ngo_name = ?";

            ps = con.prepareStatement(query);

            ps.setString(1, ngoName);

            rs = ps.executeQuery();

            boolean found = false;

            while(rs.next()) {

                found = true;

                System.out.println("----------------------------");
                System.out.println(
                    "Allocation ID : " + rs.getInt("id")
                );
                System.out.println(
                    "Food          : " + rs.getString("food_name")
                );
                System.out.println(
                    "Quantity      : " + rs.getInt("quantity_taken")
                );
                System.out.println(
                    "Donor         : " + rs.getString("donor_name")
                );
            }

            if(!found) {

                System.out.println("No food allocated yet.");
            }

        }
        catch(SQLException e) {

            System.out.println("Exception : " + e.getMessage());

        }
        finally {

            try {

                if(rs != null)
                    rs.close();

                if(ps != null)
                    ps.close();

                if(con != null)
                    con.close();

            }
            catch(SQLException e) {

                System.out.println(
                    "Closing Exception : " + e.getMessage()
                );
            }
        }
    }

    void viewDelivery(String ngoName) {

        System.out.println("\n---------- DELIVERY DETAILS ----------");

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            con = DBconnection.getConnection();

            String query =
                "SELECT d.id, r.food_name, d.assigned_time, " +
                "v.name, v.phone, v.status " +
                "FROM deliveries d " +
                "JOIN requests r ON d.request_id = r.id " +
                "JOIN volunteers v ON d.volunteer_id = v.id " +
                "WHERE r.ngo_name = ?";

            ps = con.prepareStatement(query);

            ps.setString(1, ngoName);

            rs = ps.executeQuery();

            boolean found = false;

            while(rs.next()) {

                found = true;

                System.out.println("----------------------------");
                System.out.println("Delivery ID : " + rs.getInt("id"));
                System.out.println("Food        : " + rs.getString("food_name"));
                System.out.println("Volunteer   : " + rs.getString("name"));
                System.out.println("Phone       : " + rs.getString("phone"));
                System.out.println("Status      : " + rs.getString("status"));
                System.out.println(
                    "Assigned On : " + rs.getTimestamp("assigned_time")
                );
            }

            if(!found) {

                System.out.println("Delivery not assigned yet.");
            }

        }
        catch(SQLException e) {

            System.out.println("Exception : " + e.getMessage());

        }
        finally {

            try {

                if(rs != null)
                    rs.close();

                if(ps != null)
                    ps.close();

                if(con != null)
                    con.close();

            }
            catch(SQLException e) {

                System.out.println(
                    "Closing Exception : " + e.getMessage()
                );
            }
        }
    }

    void cancelRequest(String ngoName) {

        System.out.println("\n---------- CANCEL REQUEST ----------");

        int requestId;

        System.out.print("Enter Request ID : ");

        try {
            requestId = sc.nextInt();
            sc.nextLine();
        }
        catch(Exception e) {
            System.out.println("Invalid Request ID. Please enter a number.");
            sc.nextLine();
            return;
        }

        Connection con = null;
        PreparedStatement checkPs = null;
        PreparedStatement allocationPs = null;
        PreparedStatement restorePs = null;
        PreparedStatement updateRequestPs = null;
        ResultSet rs = null;

        try {

            con = DBconnection.getConnection();
            con.setAutoCommit(false);

            String checkQuery =
                "SELECT status FROM requests WHERE id = ? AND ngo_name = ?";

            checkPs = con.prepareStatement(checkQuery);
            checkPs.setInt(1, requestId);
            checkPs.setString(2, ngoName);

            rs = checkPs.executeQuery();

            if(!rs.next()) {
                System.out.println("Request not found.");
                con.rollback();
                return;
            }

            if(!rs.getString("status").equals("Pending")) {
                System.out.println("Only pending requests can be cancelled.");
                con.rollback();
                return;
            }

            String allocationQuery =
                "SELECT donation_id, quantity_taken FROM allocations WHERE request_id = ?";

            allocationPs = con.prepareStatement(allocationQuery);
            allocationPs.setInt(1, requestId);

            ResultSet allocRs = allocationPs.executeQuery();

            if(allocRs.next()) {

                int donationId = allocRs.getInt("donation_id");
                int quantityTaken = allocRs.getInt("quantity_taken");

                String restoreQuery =
                    "UPDATE donations SET quantity = quantity + ? WHERE id = ?";

                restorePs = con.prepareStatement(restoreQuery);
                restorePs.setInt(1, quantityTaken);
                restorePs.setInt(2, donationId);

                restorePs.executeUpdate();
            }

            allocRs.close();

            String updateQuery =
                "UPDATE requests SET status = 'Cancelled' WHERE id = ?";

            updateRequestPs = con.prepareStatement(updateQuery);
            updateRequestPs.setInt(1, requestId);
            updateRequestPs.executeUpdate();

            con.commit();

            System.out.println("Request cancelled successfully.");
            System.out.println("Donation quantity restored.");

        }
        catch(SQLException e) {

            System.out.println("Exception : " + e.getMessage());

            if(con != null) {
                try {
                    con.rollback();
                }
                catch(SQLException ex) {
                    System.out.println("Rollback Exception : " + ex.getMessage());
                }
            }
        }
        finally {

            try {
                if(rs != null) rs.close();
                if(checkPs != null) checkPs.close();
                if(allocationPs != null) allocationPs.close();
                if(restorePs != null) restorePs.close();
                if(updateRequestPs != null) updateRequestPs.close();

                if(con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            }
            catch(SQLException e) {
                System.out.println("Closing Exception : " + e.getMessage());
            }
        }
    }
}