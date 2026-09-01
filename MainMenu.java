import java.util.*;

class UserRole {
    void role() {

        Scanner sc = new Scanner(System.in);
        Donor donor = new Donor(sc);
        Volunteer volunteer = new Volunteer(sc);
        NGO ngo = new NGO(sc , volunteer);

        System.out.println("\n---------- Welcome to Food Rescue Plan ----------\n");
        int choice = 0;
        
        while(choice!=4){

            System.out.println("1. Donor");
            System.out.println("2. NGO");
            System.out.println("3. Volunteer");
            System.out.println("4. Exit\n");

            System.out.print("Enter your choice : ");

            try {
                choice = sc.nextInt();
                sc.nextLine();
                System.out.println();
            }
            catch(InputMismatchException i){
                System.out.println("Please enter a valid number(1-4) only.");
                sc.nextLine();
                choice = 0;
                continue;
            }

            switch(choice) {
                case 1:
                    System.out.print("Enter Donor name : " );
                    String donorName = sc.nextLine(); 
                    donor.donorMenu(donorName);
                    break;

                case 2:
                    System.out.print("Enter NGO name : " );
                    String ngoName = sc.nextLine();
                    ngo.ngoMenu(ngoName); 
                    break;

                case 3:
                    System.out.print("Enter Volunteer name : " );
                    String volunteerName = sc.nextLine();
                    volunteer.volunteerMenu(volunteerName);
                    break;

                case 4:
                    System.out.println("Exiting from Food Rescue Plan....");
                    break;

                default:
                    System.out.println("Invalid choie chosen....");
                    System.out.println("Try Again....\n");
                    break;
            }
        }
        sc.close();
    }
}

public class MainMenu {
    public static void main(String[] args) {

        UserRole userRole = new UserRole();
        userRole.role();

    }
} 

// Ishant - Database Connectivity and Main Menu.
// Manik - NGO Class file.
// Kartik - Volunteer Class file.
// Aman - Donor Class file.