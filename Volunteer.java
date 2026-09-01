import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class Volunteer {

    Scanner sc;
    Volunteer(Scanner sc) { this.sc = sc; }

    void volunteerMenu(String volunteerName) {

        int volunteerId = getVolunteerId(volunteerName);

        if (volunteerId == -1) {
            System.out.println("Volunteer not found.");
            return;
        }

        int choice;

        do {

            System.out.println("\n========== VOLUNTEER MENU ==========");
            System.out.println("1. View My Assigned Deliveries");
            System.out.println("2. Mark My Delivery as Complete");
            System.out.println("3. Assign Volunteer to a Request");
            System.out.println("4. Logout");

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
                    viewAssignedDeliveries(volunteerId);
                    break;

                case 2:
                    markDeliveryComplete(volunteerId);
                    break;

                case 3:
                    assignManually(volunteerId);
                    break;

                case 4:
                    System.out.println("Volunteer logged out successfully.");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while(choice != 4);
    }

    int getVolunteerId(String volunteerName) {

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            con = DBconnection.getConnection();

            String query =
                "SELECT id FROM volunteers WHERE name = ?";

            ps = con.prepareStatement(query);

            ps.setString(1, volunteerName);

            rs = ps.executeQuery();

            if(rs.next()) {
                return rs.getInt("id");
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

        return -1;
    }

    void assignManually(int loggedInVolunteerId) {

        System.out.println("\n---------- ASSIGN VOLUNTEER ----------");

        int requestId;
        System.out.print("Enter Request ID to assign a volunteer : ");

        try {
            requestId = sc.nextInt();
            sc.nextLine();
        }
        catch(Exception e) {
            System.out.println("Invalid Request ID. Please enter a number.");
            sc.nextLine();
            return;
        }

        assignVolunteer(requestId, 0, loggedInVolunteerId);
    }

    void assignVolunteer(int requestId, int donationId, int loggedInVolunteerId) {

        Connection con = null;
        PreparedStatement checkPs = null;
        PreparedStatement findPs = null;
        PreparedStatement insertPs = null;
        PreparedStatement updatePs = null;
        ResultSet rs = null;

        try {

            con = DBconnection.getConnection();

            int volunteerId = -1;
            String volunteerName = null;

            // STEP 1 : check if the currently logged-in volunteer is Free
            String checkQuery =
                "SELECT name, status FROM volunteers WHERE id = ?";

            checkPs = con.prepareStatement(checkQuery);
            checkPs.setInt(1, loggedInVolunteerId);

            rs = checkPs.executeQuery();

            if(rs.next() && rs.getString("status").equals("Free")) {
                volunteerId = loggedInVolunteerId;
                volunteerName = rs.getString("name");
            }

            rs.close();

            // STEP 2 : if the logged-in volunteer is not free, pick any free volunteer
            if(volunteerId == -1) {

                String findQuery =
                    "SELECT id, name FROM volunteers " +
                    "WHERE status = 'Free' LIMIT 1";

                findPs = con.prepareStatement(findQuery);

                rs = findPs.executeQuery();

                if(!rs.next()) {
                    System.out.println("No free volunteer available.");
                    return;
                }

                volunteerId = rs.getInt("id");
                volunteerName = rs.getString("name");
            }

            // STEP 3 : insert the delivery
            String insertQuery =
                "INSERT INTO deliveries (request_id, volunteer_id) " +
                "VALUES (?, ?)";

            insertPs = con.prepareStatement(insertQuery);

            insertPs.setInt(1, requestId);
            insertPs.setInt(2, volunteerId);

            insertPs.executeUpdate();

            // STEP 4 : change that volunteer's status FREE -> BUSY
            String updateQuery =
                "UPDATE volunteers SET status = 'Busy' " +
                "WHERE id = ?";

            updatePs = con.prepareStatement(updateQuery);

            updatePs.setInt(1, volunteerId);

            updatePs.executeUpdate();

            System.out.println("\nVolunteer assigned successfully.");
            System.out.println("Volunteer    : " + volunteerName);
            System.out.println("Volunteer ID : " + volunteerId);

        }
        catch(SQLException e) {

            System.out.println("Exception : " + e.getMessage());
        }
        finally {

            try {
                if(rs != null) rs.close();
                if(checkPs != null) checkPs.close();
                if(findPs != null) findPs.close();
                if(insertPs != null) insertPs.close();
                if(updatePs != null) updatePs.close();
                if(con != null) con.close();
            }
            catch(SQLException e) {
                System.out.println("Closing Exception : " + e.getMessage());
            }
        }
    }

    void viewAssignedDeliveries(int volunteerId) {

        System.out.println("\n---------- MY ASSIGNED DELIVERIES ----------");

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            con = DBconnection.getConnection();

            String query =
                "SELECT d.id, r.food_name, r.quantity, d.assigned_time " +
                "FROM deliveries d " +
                "JOIN requests r ON d.request_id = r.id " +
                "WHERE d.volunteer_id = ?";

            ps = con.prepareStatement(query);

            ps.setInt(1, volunteerId);

            rs = ps.executeQuery();

            boolean found = false;

            while(rs.next()) {

                found = true;

                System.out.println("----------------------------");
                System.out.println("Delivery ID : " + rs.getInt("id"));
                System.out.println("Food        : " + rs.getString("food_name"));
                System.out.println("Quantity    : " + rs.getInt("quantity"));
                System.out.println("Assigned On : " + rs.getDate("assigned_time"));
            }

            if(!found) {
                System.out.println("No assigned deliveries.");
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

    void markDeliveryComplete(int volunteerId) {

        System.out.println("\n---------- COMPLETE DELIVERY ----------");

        Connection con = null;
        PreparedStatement ps = null;

        try {

            con = DBconnection.getConnection();

            String query =
                "UPDATE volunteers SET status = 'Free' " +
                "WHERE id = ?";

            ps = con.prepareStatement(query);

            ps.setInt(1, volunteerId);

            int result = ps.executeUpdate();

            if(result > 0) {
                System.out.println("Delivery completed successfully.");
                System.out.println("Volunteer status changed: BUSY -> FREE");
            }
            else {
                System.out.println("Volunteer not found.");
            }

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
}