package utb.fai;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class SocketHandler {
	/** mySocket je socket, o který se bude tento SocketHandler starat */
	Socket mySocket;

	/** client ID je øetìzec ve formátu <IP_adresa>:<port> */
	String clientID;

	volatile String name = null;

	Set<String> groups = Collections.synchronizedSet(new LinkedHashSet<String>());

	/**
	 * activeHandlers je reference na mnoinu vech právì bìících SocketHandlerù.
	 * Potøebujeme si ji udrovat, abychom mohli zprávu od tohoto klienta
	 * poslat vem ostatním!
	 */
	ActiveHandlers activeHandlers;

	/**
	 * messages je fronta pøíchozích zpráv, kterou musí mít kaý klient svoji
	 * vlastní - pokud bude je pøetíená nebo nefunkèní klientova sí,
	 * èekají zprávy na doruèení právì ve frontì messages
	 */
	ArrayBlockingQueue<String> messages = new ArrayBlockingQueue<String>(20);

	/**
	 * startSignal je synchronizaèní závora, která zaøizuje, aby oba tasky
	 * OutputHandler.run() a InputHandler.run() zaèaly ve stejný okamik.
	 */
	CountDownLatch startSignal = new CountDownLatch(2);

	/** outputHandler.run() se bude starat o OutputStream mého socketu */
	OutputHandler outputHandler = new OutputHandler();
	/** inputHandler.run() se bude starat o InputStream mého socketu */
	InputHandler inputHandler = new InputHandler();
	/**
	 * protoe v outputHandleru nedovedu detekovat uzavøení socketu, pomùe mi
	 * inputFinished
	 */
	volatile boolean inputFinished = false;

	public SocketHandler(Socket mySocket, ActiveHandlers activeHandlers) {
		this.mySocket = mySocket;
		clientID = mySocket.getInetAddress().toString() + ":" + mySocket.getPort();
		this.activeHandlers = activeHandlers;
		groups.add("public");
	}

	void reply(String message){
		if (!messages.offer(message))
			System.err.println("Client %s message queue is full, dropping the message!");
	}
	void trySetName(String newName){
		newName = newName.trim();
		if (newName.isEmpty() || newName.contains(" ") || newName.startsWith("#")){
			reply("Invalid name. Name must not be empty and must not contain spaces.");
			return;
		}
		if (activeHandlers.setName(this, newName)) reply("Your name is now " + newName);
		else reply("Name " + newName + " is already taken. Choose another one.");
	}

	void processLine(String line){
		line = line.trim();
		if (line.isEmpty()) return;

		//1) klient nema meno -> prvy riadok je meno
		if (name == null){
			if (line.startsWith("#setMyName"))
				line = line.substring("#setMyName".length());
			trySetName(line);
			return;
		}

		//2) prikazy
		if (line.startsWith("#setMyName")){
			trySetName(line.substring("#setMyName".length()));

		}else if (line.startsWith("#sendPrivate")){
			String[] parts = line.split("\\s+", 3);
			if (parts.length < 3){
				reply("Usage: #sendPrivate <name> <message>");
				return;
			}
			if(!activeHandlers.sendPrivate(parts[1], "[" + name + "] >> " + parts[2]))
				reply("User " +parts[1] + " does not exist.");

		}else if (line.startsWith("#join")){
			String group = line.substring("#join".length()).trim();
			if(group.isEmpty() || group.contains(" ")){
				reply("Usage: #join <group>");
				return;
			}
			groups.add(group);

		}else if(line.startsWith("#leave")){
			String group = line.substring("#leave".length()).trim();
			groups.remove(group);

		}else if(line.equals("#groups")){
			synchronized (groups){
				reply(String.join(",",groups));
			}

		//3) bezna sprava
		}else{
			String msg = "[" + name + "] >> " + line;
			System.out.println(msg);
			activeHandlers.sendMessageToAll(this, msg);
		}
	}

	class OutputHandler implements Runnable{
		public void run() {
			OutputStreamWriter writer;
			try{
				startSignal.countDown();
				startSignal.await();
				writer = new OutputStreamWriter(mySocket.getOutputStream(), "UTF-8");
				writer.write("Enter your name:\r\n");
				writer.flush();
				while (!inputFinished){
					String m = messages.take();
					if (inputFinished)
						break;
					writer.write(m + "\r\n");
					writer.flush();
				}
			} catch (IOException e){
				e.printStackTrace();
			} catch (InterruptedException e){
				e.printStackTrace();
			}
			System.err.println("DBG>Output handler for " + clientID + " has finished.");
		}
	}

	class InputHandler implements Runnable{
		public void run(){
			try{
				startSignal.countDown();
				startSignal.await();
				activeHandlers.add(SocketHandler.this);
				BufferedReader reader = new BufferedReader(
					new InputStreamReader(mySocket.getInputStream(), "UTF-8"));
				String request;
				while ((request = reader.readLine()) != null){
					processLine(request);
				}
			} catch(IOException e){
				e.printStackTrace();
			} catch (InterruptedException e){
				e.printStackTrace();
			} finally {
				inputFinished = true;
				messages.offer("OutputHandler, wakeup and die!");
				activeHandlers.remove(SocketHandler.this);
				try{
					mySocket.close();
				} catch (IOException e){
				}
			}
			System.err.println("DBG>Input handle for " + clientID + " has finished");
		}
	}
}
