package staff;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.time.LocalDate;
import java.util.Set;
import java.util.Collections;

import hotel.BookingDetail;
import hotel.BookingManagerInterface;

public class BookingClient extends AbstractScriptedSimpleTest {

	private BookingManagerInterface bm;

	public static void main(String[] args) throws Exception {
		BookingClient client = new BookingClient(args);
		client.run();
	}

	/***************
	 * CONSTRUCTOR *
	 ***************/
	public BookingClient(String[] args) {
		try {
			String host = (args.length > 0) ? args[0] : "localhost";
			Registry registry = LocateRegistry.getRegistry(host, 1099);
			bm = (BookingManagerInterface) registry.lookup("BookingService");
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}

	@Override
	public boolean isRoomAvailable(Integer roomNumber, LocalDate date) {
		try {
			return bm.isRoomAvailable(roomNumber, date);
		} catch (RemoteException e) {
			System.err.println("RMI Error: " + e.getMessage());
			System.exit(1);
			return false; // Unreachable, but required by compiler
		}
	}

	@Override
	public void addBooking(BookingDetail bookingDetail) throws Exception {
		try {
			bm.addBooking(bookingDetail);
		} catch (RemoteException e) {
			System.err.println("RMI Error: " + e.getMessage());
			System.exit(1);
		}
	}

	@Override
	public Set<Integer> getAvailableRooms(LocalDate date) {
		try {
			return bm.getAvailableRooms(date);
		} catch (RemoteException e) {
			System.err.println("RMI Error: " + e.getMessage());
			System.exit(1);
			return Collections.emptySet(); // Unreachable, but required by compiler
		}
	}

	@Override
	public Set<Integer> getAllRooms() {
		try {
			return bm.getAllRooms();
		} catch (RemoteException e) {
			System.err.println("RMI Error: " + e.getMessage());
			System.exit(1);
			return Collections.emptySet(); // Unreachable, but required by compiler
		}
	}
}
