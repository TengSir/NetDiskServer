package edu.hbuas.netdiskserver.control;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import edu.hbuas.netdisk.model.Message;
/**
 * 封装一个网盘服务器的服务类
 * @author Lenovo
 *
 */
public class Server {
	private ServerSocket server;
	{
		try {
			server=new ServerSocket(9999);
			System.out.println("服务器启动成功");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public Server() {
		try {
			while(true) {
			Socket  client=server.accept();
			ObjectInputStream in=new ObjectInputStream(client.getInputStream());
			ObjectOutputStream  out=new ObjectOutputStream(client.getOutputStream());
			System.out.println(client.getInetAddress().getHostAddress()+"链接进来了！");
			ClientThread  c=new ClientThread(in, out);
			c.start();//每次链接进来一个客户端，我们单独开启一个线程让这个客户端使用独立多线程分支和服务器做io通讯
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 封装一个内部类，这个内部类主要是开启一个线程和每个客户端独立通讯的类
	 */
	private class  ClientThread extends Thread{
		private ObjectInputStream in;
		private 	ObjectOutputStream  out;
		
		public ClientThread(ObjectInputStream in, ObjectOutputStream out) {
			super();
			this.in = in;
			this.out = out;
		}


		@Override
		public void run() {
			/**
			 * 这个run方法是每个客户端的独立线程，用来单独和这个客户端通讯
			 */
			try {
				Message  message=(Message)in.readObject();
				System.out.println("server recived one message:"+message);
				
				/**
				 * 接到消息后应该判断消息类型，执行不同的操作
				 */
				switch (message.getType()) {
				case UPLOAD:
				{
					String fileName=message.getFilename();
					File userDir=new File("D:/netDiskServer/"+message.getFromUser());
					if(!userDir.exists())userDir.mkdir();
					FileOutputStream  out=new FileOutputStream(userDir.getAbsolutePath()+"/"+fileName);
					byte[] bs=new byte[1024];
					int length=-1;
					while((length=in.read(bs))!=-1) {
						out.write(bs,0,length);
						out.flush();
					}
					out.close();
					in.close();
					System.out.println(message.getFromUser()+"的文件["+message.getFileLength()+"],上传完毕！");
					
					break;
				}

				case DOWNLOAD:
				{

					break;
				}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		}
	}
	
	
	public static void main(String[] args) {
		new Server();
	}
}
