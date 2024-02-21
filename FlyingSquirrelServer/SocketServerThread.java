/**处理与客户端的连接*/

import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketServerThread extends Thread {
    private Socket socket;
    private LinkedBlockingQueue<byte[]> queue;
    public SocketServerThread(Socket socket,LinkedBlockingQueue<byte[]> queue) {
        this.socket = socket;
        this.queue = queue;
    }
 
  
    public void run() {
        try {
           
            InputStream  inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            byte buffer[]= new byte [2048];

            String message = "Message OK"; //回答客户端的内容
            byte[] out = message.getBytes();
            while (true) {
                    
                int len = inputStream.read(buffer);
                int size = 0;
                /** 不断的取出报告符数据
                 * 防粘包 半包 第一字节为数据长度                  
                 * */ 
                while (len > 1) {
                    byte tx[]= new byte [buffer[size]];//建一个数组
                    System.arraycopy(buffer, size + 1, tx, 0, tx.length);//copy数据
                    queue.put(tx);
                    size = tx.length + 1;
                    len = len-size;
                }   
                /**回答一下客户端*/
                outputStream.write(out);
                outputStream.flush();// 刷新缓冲区

            }
        
        }catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
    
            e.printStackTrace();
        } 


          
       
    } 


}