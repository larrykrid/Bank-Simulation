import java.util.concurrent.Semaphore;
import java.util.Random;

public class My_Threads {
	public static boolean open = true;
	public static int tell_number = 3;
	public static int client_number = 10;
	public static Random rand = new Random();
	public static TellerThread[] tellers = new TellerThread[tell_number];
	public static ClientThread[] clients = new ClientThread[client_number];
	
	public static Semaphore manLock = new Semaphore(1);
	public static Semaphore safeLock = new Semaphore(2);
	public static Semaphore tellLock = new Semaphore(0);//don't think this is needed
	public static Semaphore lineLock = new Semaphore(1);
	public static Semaphore waitLock = new Semaphore(0);
	public static Semaphore clientWaitLock = new Semaphore(0);
	public static Semaphore tellerWaitLock = new Semaphore(0);
	public static Semaphore closeLock = new Semaphore(0);

	
	public static void main(String args[]) {
		
		
		//Create 3 teller threads
		for (int i = 0; i < tell_number; i++) {
			tellers[i] = new TellerThread(i);
			tellers[i].start();
		}
		
		//create 100 client threads
		for (int i = 0; i < client_number; i++) {
			clients[i] = new ClientThread(i, rand.nextInt(2));
			clients[i].start();
		}
		//how do I get the teller ready to help the next client?
		while(open) {
			if (closeLock.availablePermits() == tell_number) {
				open = false;
				System.out.println("Bank closes");
			}
		}
		
	}
		
	public static class TellerThread extends Thread {
		int id;
		int clientId;
		String clientAction;
		boolean available = false;
		//Need to add a semaphore specifically to the thread. This will make it so they know whos semaphore is being used and not used
		
		
		TellerThread(int id) {
			this.id = id;
		}
		
		public void ClientInfo(int cId, String action) {
			this.clientId = cId;
			this.clientAction = action;
		}
		
		public void run() {
			while(open) {
				try {
					System.out.println("Teller " + id + " is available");
					available = true;
					tellLock.release(); //add available teller
					if (client_number == 0) {
						waitLock.release(tell_number);
						break;
					}
					waitLock.acquire();
					if (client_number == 0) {
						break;
					}
					//help a client that comes to it
					System.out.println("Teller " + id + " is serving Client " + clientId);
					System.out.println("Teller " + id + " is processing Client's " + clientAction);
					if (clientAction.equals("Withdraw")){
						manLock.acquire();
						System.out.println("Teller " + id + " is getting the manager's approval");
						Thread.sleep(rand.nextInt((300-50)+1) + 50);
						System.out.println("Teller " + id + " got the manager's approval");
						manLock.release();
						System.out.println("Teller " + id + " is going to the safe");
						safeLock.acquire();
						System.out.println("Teller " + id + " is using the safe");
						Thread.sleep(rand.nextInt((500-100) + 1) + 100);
						System.out.println("Teller " + id + " is done using the safe");
						safeLock.release();
						System.out.println("Teller " + id + " notifies Client " + clientId);
					
					//Need the action to notify the client
						clients[clientId].setWait(false);
						clientWaitLock.release();
					//Need the action that lets the teller know client leaves
						tellerWaitLock.acquire();
						System.out.println("Teller " + id + " has finished serving Client " + clientId);
					}
					else if (clientAction.equals("Deposit")){
						System.out.println("Teller " + id + " is going to the safe");
						safeLock.acquire();
						System.out.println("Teller " + id + " is using the safe");
						Thread.sleep(rand.nextInt((500-100) + 1) + 100);
						System.out.println("Teller " + id + " is done using the safe");
						safeLock.release();
						System.out.println("Teller " + id + " notifies Client " + clientId);
					
					//Need the action to notify the client
						clients[clientId].setWait(false);
						clientWaitLock.release();
					//Need the action that lets the teller know client leaves
						tellerWaitLock.acquire();
					
						System.out.println("Teller " + id + " has finished serving Client " + clientId);
					}
					client_number--;
				} catch(Exception e){
					System.err.println("Error in Thread " + id + ": " + e);
				}
			}
			//Once every client is helped, Tellers close
				System.out.println("Teller " + id + " Closes");
				closeLock.release();
		}
	}
	
	public static class ClientThread extends Thread {
		int id;
		int tellerId;
		String action;
		boolean wait = true;
		
		ClientThread(int id, int act) {
			this.id = id;
			if (act == 0) {
				action = "Withdraw";
			}
			else if (act == 1) {
				action = "Deposit";
			}
		}
		
		public void setWait (boolean bool) {
			this.wait = bool;
		}
		
		public void run() {
			try {
				System.out.println("Client " + id + " waits in line to make a " + action);
				lineLock.acquire();
				tellLock.acquire();
				for(int i = 0; i < tell_number; i++) {
					if(tellers[i].available == true) {
						tellers[i].ClientInfo(id, action);
						tellers[i].available = false;
						tellerId = tellers[i].id;
						break;
					}
				}
				System.out.println("Client " + id + " goes to teller " + tellerId);
				lineLock.release();
				waitLock.release();
				while(wait) {
					clientWaitLock.acquire();
					if(wait != false) {
						clientWaitLock.release();
					}
				}
				System.out.println("Client " + id + " is done");
				System.out.println("Client " + id + " leaves");
				tellerWaitLock.release();
			} catch(Exception e){
				System.err.println("Error in Thread " + id + ": " + e);
			}
		}
	}
}