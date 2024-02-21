
import java.awt.AWTException;
import java.awt.Robot;
import java.util.concurrent.LinkedBlockingQueue;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.MouseInfo;

public class QueueThread extends Thread {
   
    private LinkedBlockingQueue<byte[]> queue;
    public QueueThread(LinkedBlockingQueue<byte[]> queue) {
        this.queue = queue;
    }
    

    /**读队列数据，*/
    public void run() {
       
        try {
           
            byte buffer[]= new byte [2048];
        
            Robot robot = new Robot();
         
            while (true) {  
                buffer = queue.take();
              
                if(buffer.length == 8){
                    /**处理键盘事件*/
                    keyboard(robot,buffer);
                    // robot.delay(20);//这儿要不要休息一下，在硬件USB-HID是报告一次数据后是休息20ms的
        
                }else if(buffer.length == 5){
                    /**处理鼠标事件*/
                    mouse(robot,buffer);
                    // robot.delay(20);
                }
                // Thread.sleep(20);//不要太快取数据


            }  

        }

        catch (InterruptedException e) {
            e.printStackTrace();
        } 
        catch (AWTException e) {
            e.printStackTrace();
        }

    }
   
 

    private void keyboard(Robot robot, byte data[]){
       
        /**
            客户端发送的数据是USB-HID标准报告符  
        bit0: 左 ctrl 键    bit1: 左 shift 键   bit2: 左 Alt 键 bit3: 左 Window 键
        bit4: 右 ctrl 键    bit5: 右 shift 键   bit6: 右 Alt 键 bit7: 右 Window 键  
        java Robot 按键采用ascii码
        参考网址： https://www.bejson.com/othertools/keycodes/    
        */

        int keyDown [] = new int[16];//记录有那些键按下了
        int num = 1;
        /**byte 0 字节内容*/
        if (data[0] != 0) {
            StringBuffer bitString = new StringBuffer();
            bitString.append((data[0]>>7)&0x1)
                     .append((data[0]>>6)&0x1)
                     .append((data[0]>>5)&0x1)
                     .append((data[0]>>4)&0x1)
                     .append((data[0]>>3)&0x1)
                     .append((data[0]>>2)&0x1)
                     .append((data[0]>>1)&0x1)
                     .append((data[0]>>0)&0x1);
            // System.out.println("  "+ bitString.toString());
            if(bitString.toString().charAt(7) == '1'){
                robot.keyPress(KeyEvent.VK_CONTROL); 
                keyDown[num] = KeyEvent.VK_CONTROL;
                // robot.keyRelease(KeyEvent.VK_CONTROL); 
                num++;
            }
            if(bitString.toString().charAt(6) == '1'){
                robot.keyPress(KeyEvent.VK_SHIFT); 
                // robot.keyRelease(KeyEvent.VK_SHIFT); 
                keyDown[num] = KeyEvent.VK_SHIFT;
                num++;
            }
            if(bitString.toString().charAt(5) == '1'){
                robot.keyPress(KeyEvent.VK_ALT); 
                // robot.keyRelease(KeyEvent.VK_ALT); 
                keyDown[num] = KeyEvent.VK_ALT;
                num++;
            }
            if(bitString.toString().charAt(4) == '1'){
                robot.keyPress(KeyEvent.VK_WINDOWS); 
                // robot.keyRelease(KeyEvent.VK_WINDOWS);
                keyDown[num] = KeyEvent.VK_WINDOWS;
                num++;

            }
            if(bitString.toString().charAt(3) == '1'){
                robot.keyPress(KeyEvent.VK_CONTROL); 
                // robot.keyRelease(KeyEvent.VK_CONTROL);
                keyDown[num] = KeyEvent.VK_CONTROL;
                num++;
            }
            if(bitString.toString().charAt(2) == '1'){
                robot.keyPress(KeyEvent.VK_SHIFT); 
                // robot.keyRelease(KeyEvent.VK_SHIFT);
                keyDown[num] = KeyEvent.VK_SHIFT;
                num++;

            }
            if(bitString.toString().charAt(1) == '1'){
                robot.keyPress(KeyEvent.VK_ALT); 
                // robot.keyRelease(KeyEvent.VK_ALT);
                keyDown[num] = KeyEvent.VK_ALT;
                num++;

            }
            if(bitString.toString().charAt(0) == '1'){
                robot.keyPress(KeyEvent.VK_WINDOWS); 
                // robot.keyRelease(KeyEvent.VK_WINDOWS); 
                keyDown[num] = KeyEvent.VK_WINDOWS;
                num++;
            }


        }

        /**按键码*/
        for(int i=2;i<data.length;i++){
            switch (data[i]) {
                /**字母按键*/
                case 0x04: //A
                    robot.keyPress(KeyEvent.VK_A); 
                    // robot.keyRelease(KeyEvent.VK_A);
                    keyDown[num] = KeyEvent.VK_A;
                    num++;
                    break;
                case 0x05: //B
                    robot.keyPress(KeyEvent.VK_B); 
                    // robot.keyRelease(KeyEvent.VK_B);
                    keyDown[num] = KeyEvent.VK_B;
                    num++;
                    break;
                case 0x06: //C
                    robot.keyPress(KeyEvent.VK_C); 
                    // robot.keyRelease(KeyEvent.VK_C);
                    keyDown[num] = KeyEvent.VK_C;
                    num++;
                    break;
                case 0x07: //D
                    robot.keyPress(KeyEvent.VK_D); 
                    // robot.keyRelease(KeyEvent.VK_D);
                    keyDown[num] = KeyEvent.VK_D;
                    num++;
                    break;
                case 0x08: //E
                    robot.keyPress(KeyEvent.VK_E); 
                    // robot.keyRelease(KeyEvent.VK_E);
                    keyDown[num] = KeyEvent.VK_E;
                    num++;
                    break;
                case 0x09: //F
                    robot.keyPress(KeyEvent.VK_F); 
                    // robot.keyRelease(KeyEvent.VK_F);
                    keyDown[num] = KeyEvent.VK_F;
                    num++;
                    break;
                case 0X0A: //G
                    robot.keyPress(KeyEvent.VK_G); 
                    // robot.keyRelease(KeyEvent.VK_G);
                    keyDown[num] = KeyEvent.VK_G;
                    num++;
                    break;
                case 0x0B: //H
                    robot.keyPress(KeyEvent.VK_H); 
                    // robot.keyRelease(KeyEvent.VK_H);
                    keyDown[num] = KeyEvent.VK_H;
                    num++;
                    break;
                case 0x0C: //I
                    robot.keyPress(KeyEvent.VK_I); 
                    // robot.keyRelease(KeyEvent.VK_I);
                    keyDown[num] = KeyEvent.VK_I;
                    num++;
                    break;
                case 0x0D: //J
                    robot.keyPress(KeyEvent.VK_J); 
                    // robot.keyRelease(KeyEvent.VK_J);
                    keyDown[num] = KeyEvent.VK_J;
                    num++;
                    break;
                case 0x0E: //K
                    robot.keyPress(KeyEvent.VK_K); 
                    // robot.keyRelease(KeyEvent.VK_K);
                    keyDown[num] = KeyEvent.VK_K;
                    num++;
                    break;
                case 0x0F: //L
                    robot.keyPress(KeyEvent.VK_L); 
                    // robot.keyRelease(KeyEvent.VK_L);
                    keyDown[num] = KeyEvent.VK_L;
                    num++;
                    break;
                 case 0x10: //M
                    robot.keyPress(KeyEvent.VK_M); 
                    // robot.keyRelease(KeyEvent.VK_M);
                    keyDown[num] = KeyEvent.VK_M;
                    num++;
                    break;
                case 0x11: //N
                    robot.keyPress(KeyEvent.VK_N); 
                    // robot.keyRelease(KeyEvent.VK_N);
                    keyDown[num] = KeyEvent.VK_N;
                    num++;
                    break;
                case 0x12: //O
                    robot.keyPress(KeyEvent.VK_O); 
                    // robot.keyRelease(KeyEvent.VK_O);
                    keyDown[num] = KeyEvent.VK_O;
                    num++;
                    break;
                case 0x13: //P
                    robot.keyPress(KeyEvent.VK_P); 
                    // robot.keyRelease(KeyEvent.VK_P);
                    keyDown[num] = KeyEvent.VK_P;
                    num++;
                    break;
                case 0x14: //Q
                    robot.keyPress(KeyEvent.VK_Q); 
                    // robot.keyRelease(KeyEvent.VK_Q);
                    keyDown[num] = KeyEvent.VK_Q;
                    num++;
                    break;
                case 0x15: //R
                    robot.keyPress(KeyEvent.VK_R); 
                    // robot.keyRelease(KeyEvent.VK_R);    
                    keyDown[num] = KeyEvent.VK_R;
                    num++;
                    break;
                case 0x16: //S
                    robot.keyPress(KeyEvent.VK_S); 
                    // robot.keyRelease(KeyEvent.VK_S);
                    keyDown[num] = KeyEvent.VK_S;
                    num++;
                    break;
                case 0x17: //T
                    robot.keyPress(KeyEvent.VK_T); 
                    // robot.keyRelease(KeyEvent.VK_T);
                    keyDown[num] = KeyEvent.VK_T;
                    num++;
                    break;
                case 0x18: //U
                    robot.keyPress(KeyEvent.VK_U); 
                    // robot.keyRelease(KeyEvent.VK_U);
                    keyDown[num] = KeyEvent.VK_U;
                    num++;
                    break;
                case 0x19: //V
                    robot.keyPress(KeyEvent.VK_V); 
                    // robot.keyRelease(KeyEvent.VK_V);
                    keyDown[num] = KeyEvent.VK_V;
                    num++;
                    break;
                case 0x1A: //W
                    robot.keyPress(KeyEvent.VK_W); 
                    // robot.keyRelease(KeyEvent.VK_W);
                    keyDown[num] = KeyEvent.VK_W;
                    num++;
                    break;
                case 0x1B: //X
                    robot.keyPress(KeyEvent.VK_X); 
                    // robot.keyRelease(KeyEvent.VK_X);
                    keyDown[num] = KeyEvent.VK_X;
                    num++;
                    break;
                case 0x1C: //Y
                    robot.keyPress(KeyEvent.VK_Y); 
                    // robot.keyRelease(KeyEvent.VK_Y);
                    keyDown[num] = KeyEvent.VK_Y;
                    num++;
                    break;
                case 0x1D: //Z
                    robot.keyPress(KeyEvent.VK_Z); 
                    // robot.keyRelease(KeyEvent.VK_Z);
                    keyDown[num] = KeyEvent.VK_Z;
                    num++;
                    break;
                /**数字按键*/
                case 0x1E: //1
                    robot.keyPress(KeyEvent.VK_1); 
                    // robot.keyRelease(KeyEvent.VK_1);
                    keyDown[num] = KeyEvent.VK_1;
                    num++;
                    break;
                case 0x1F: //2
                    robot.keyPress(KeyEvent.VK_2); 
                    // robot.keyRelease(KeyEvent.VK_2);
                    keyDown[num] = KeyEvent.VK_2;
                    num++;
                    break;
                case 0x20: //3
                    robot.keyPress(KeyEvent.VK_3); 
                    // robot.keyRelease(KeyEvent.VK_3);
                    keyDown[num] = KeyEvent.VK_3;
                    num++;
                    break;
                case 0x21: //4
                    robot.keyPress(KeyEvent.VK_4); 
                    // robot.keyRelease(KeyEvent.VK_4);
                    keyDown[num] = KeyEvent.VK_4;
                    num++;
                    break;
                case 0x22: //5
                    robot.keyPress(KeyEvent.VK_5); 
                    // robot.keyRelease(KeyEvent.VK_5);
                    keyDown[num] = KeyEvent.VK_5;
                    num++;
                    break;
                case 0x23: //6
                    robot.keyPress(KeyEvent.VK_6); 
                    // robot.keyRelease(KeyEvent.VK_6);
                    keyDown[num] = KeyEvent.VK_6;
                    num++;
                    break;
                case 0x24: //7
                    robot.keyPress(KeyEvent.VK_7); 
                    // robot.keyRelease(KeyEvent.VK_7);
                    keyDown[num] = KeyEvent.VK_7;
                    num++;
                    break;
                case 0x25: //8
                    robot.keyPress(KeyEvent.VK_8); 
                    // robot.keyRelease(KeyEvent.VK_8);
                    keyDown[num] = KeyEvent.VK_9;
                    num++;
                    break;
                case 0x26: //9
                    robot.keyPress(KeyEvent.VK_9); 
                    // robot.keyRelease(KeyEvent.VK_9);
                    keyDown[num] = KeyEvent.VK_9;
                    num++;
                    break;
                case 0x27: //0
                    robot.keyPress(KeyEvent.VK_0); 
                    // robot.keyRelease(KeyEvent.VK_0);
                    keyDown[num] = KeyEvent.VK_0;
                    num++;
                    break;
                /**功能按键*/ 

                case 0x28: //回车
                    robot.keyPress(KeyEvent.VK_ENTER); 
                    // robot.keyRelease(KeyEvent.VK_ENTER);
                    keyDown[num] = KeyEvent.VK_ENTER;
                    num++;
                    break;
                case 0x29: //ESC
                    robot.keyPress(KeyEvent.VK_ESCAPE); 
                    // robot.keyRelease(KeyEvent.VK_ESCAPE);
                    keyDown[num] = KeyEvent.VK_ESCAPE;
                    num++;
                    break;
                case 0x2A: //退格
                    robot.keyPress(KeyEvent.VK_BACK_SPACE); 
                    // robot.keyRelease(KeyEvent.VK_BACK_SPACE);
                    keyDown[num] = KeyEvent.VK_BACK_SPACE;
                    num++;
                    break;
                case 0x2B: //TAB
                    robot.keyPress(KeyEvent.VK_TAB); 
                    // robot.keyRelease(KeyEvent.VK_TAB);
                    keyDown[num] = KeyEvent.VK_TAB;
                    num++;
                    break;
                case 0x2C: //空格
                    robot.keyPress(KeyEvent.VK_SPACE); 
                    // robot.keyRelease(KeyEvent.VK_SPACE);
                    keyDown[num] = KeyEvent.VK_SPACE;
                    num++;
                    break;
                case 0x2D: // -_
                    robot.keyPress(KeyEvent.VK_MINUS); 
                    // robot.keyRelease(KeyEvent.VK_MINUS);
                    keyDown[num] = KeyEvent.VK_MINUS;
                    num++;
                    break;
                case 0x2E: // + =
                    robot.keyPress(KeyEvent.VK_EQUALS); 
                    // robot.keyRelease(KeyEvent.VK_EQUALS);
                    keyDown[num] = KeyEvent.VK_EQUALS;
                    num++;
                    break;
                case 0x2F: // { [ 
                    robot.keyPress(KeyEvent.VK_OPEN_BRACKET); 
                    // robot.keyRelease(KeyEvent.VK_OPEN_BRACKET);
                    keyDown[num] = KeyEvent.VK_OPEN_BRACKET;
                    num++;
                    break;
                case 0x30: // } ]
                    robot.keyPress(KeyEvent.VK_CLOSE_BRACKET); 
                    // robot.keyRelease(KeyEvent.VK_CLOSE_BRACKET);
                    keyDown[num] = KeyEvent.VK_CLOSE_BRACKET;
                    num++;
                    break;
                case 0x31: // \ |
                    robot.keyPress(KeyEvent.VK_BACK_SLASH); 
                    // robot.keyRelease(KeyEvent.VK_BACK_SLASH);
                    keyDown[num] = KeyEvent.VK_BACK_SLASH;
                    num++;
                    break;
                 case 0x33: // ;:
                    robot.keyPress(KeyEvent.VK_SEMICOLON); 
                    // robot.keyRelease(KeyEvent.VK_SEMICOLON);
                    keyDown[num] = KeyEvent.VK_SEMICOLON;
                    num++;
                    break;
                case 0x34: // 引号 " '
                    robot.keyPress(KeyEvent.VK_QUOTE); 
                    // robot.keyRelease(KeyEvent.VK_QUOTE);
                    keyDown[num] = KeyEvent.VK_QUOTE;
                    num++;
                    break;
                case 0x35: // 波浪号 ~
                    robot.keyPress(KeyEvent.VK_BACK_QUOTE); 
                    // robot.keyRelease(KeyEvent.VK_BACK_QUOTE);
                    keyDown[num] = KeyEvent.VK_BACK_QUOTE;
                    num++;
                    break;
                case 0x36: // <, 
                    robot.keyPress(KeyEvent.VK_COMMA); 
                    // robot.keyRelease(KeyEvent.VK_COMMA);
                    keyDown[num] = KeyEvent.VK_COMMA;
                    num++;
                    break;
                case 0x37: // >.
                    robot.keyPress(KeyEvent.VK_PERIOD); 
                    // robot.keyRelease(KeyEvent.VK_PERIOD);
                    keyDown[num] = KeyEvent.VK_PERIOD;
                    num++;
                    break;
                case 0x38: // ？ / 
                    robot.keyPress(KeyEvent.VK_SLASH); 
                    // robot.keyRelease(KeyEvent.VK_SLASH);
                    keyDown[num] = KeyEvent.VK_SLASH;
                    num++;
                    break;
                case 0x39: //大小写转换
                    robot.keyPress(KeyEvent.VK_CAPS_LOCK); 
                    // robot.keyRelease(KeyEvent.VK_CAPS_LOCK);
                    keyDown[num] = KeyEvent.VK_CAPS_LOCK;
                    num++;
                    break;
                
                case 0x3A: //F1
                    robot.keyPress(KeyEvent.VK_F1); 
                    // robot.keyRelease(KeyEvent.VK_F1);
                    keyDown[num] = KeyEvent.VK_F1;
                    num++;
                    break;
                case 0x3B: //F2
                    robot.keyPress(KeyEvent.VK_F2); 
                    // robot.keyRelease(KeyEvent.VK_F2);
                    keyDown[num] = KeyEvent.VK_F2;
                    num++;
                    break;
                case 0x3C: //F3
                    robot.keyPress(KeyEvent.VK_F3); 
                    // robot.keyRelease(KeyEvent.VK_F3);
                    keyDown[num] = KeyEvent.VK_F3;
                    num++;
                    break;
                case 0x3D: //F4
                    robot.keyPress(KeyEvent.VK_F4); 
                    // robot.keyRelease(KeyEvent.VK_F4);
                    keyDown[num] = KeyEvent.VK_F4;
                    num++;
                    break;
                case 0x3E: //F5
                    robot.keyPress(KeyEvent.VK_F5); 
                    // robot.keyRelease(KeyEvent.VK_F5);
                    keyDown[num] = KeyEvent.VK_F5;
                    num++;
                    break;
                case 0x3F: //F6
                    robot.keyPress(KeyEvent.VK_F6); 
                    // robot.keyRelease(KeyEvent.VK_F6);
                    keyDown[num] = KeyEvent.VK_F6;
                    num++;
                    break;                            
                case 0x40: //F7
                    robot.keyPress(KeyEvent.VK_F7); 
                    // robot.keyRelease(KeyEvent.VK_F7);
                    keyDown[num] = KeyEvent.VK_F7;
                    num++;
                    break;
                case 0x41: //F8
                    robot.keyPress(KeyEvent.VK_F8); 
                    // robot.keyRelease(KeyEvent.VK_F8);
                    keyDown[num] = KeyEvent.VK_F8;
                    num++;
                    break;
                case 0x42: //F9
                    robot.keyPress(KeyEvent.VK_F9); 
                    // robot.keyRelease(KeyEvent.VK_F9);
                    keyDown[num] = KeyEvent.VK_F9;
                    num++;
                    break;
                case 0x43: //F10
                    robot.keyPress(KeyEvent.VK_F10); 
                    // robot.keyRelease(KeyEvent.VK_F10);
                    keyDown[num] = KeyEvent.VK_F10;
                    num++;
                    break;
                case 0x44: //F11
                    robot.keyPress(KeyEvent.VK_F11); 
                    // robot.keyRelease(KeyEvent.VK_F11);
                    keyDown[num] = KeyEvent.VK_F11;
                    num++;
                    break;
                case 0x45: //F12
                    robot.keyPress(KeyEvent.VK_F12); 
                    // robot.keyRelease(KeyEvent.VK_F12);
                    keyDown[num] = KeyEvent.VK_F12;
                    num++;
                    break; 
                case 0x4C: //删除
                    robot.keyPress(KeyEvent.VK_DELETE); 
                    // robot.keyRelease(KeyEvent.VK_DELETE);
                    keyDown[num] = KeyEvent.VK_DELETE;
                    num++;
                    break;
                case 0x4F: //方向 RIGHT
                    robot.keyPress(KeyEvent.VK_RIGHT); 
                    // robot.keyRelease(KeyEvent.VK_RIGHT);
                    keyDown[num] = KeyEvent.VK_RIGHT;
                    num++;
                    break;
                case 0x50: //方向 LEFT
                    robot.keyPress(KeyEvent.VK_LEFT); 
                    // robot.keyRelease(KeyEvent.VK_LEFT);
                    keyDown[num] = KeyEvent.VK_LEFT;
                    num++;
                    break;
                 case 0x51: //方向 DOWN
                    robot.keyPress(KeyEvent.VK_DOWN); 
                    // robot.keyRelease(KeyEvent.VK_DOWN);
                    keyDown[num] = KeyEvent.VK_DOWN;
                    num++;
                    break;
                case 0x52: //方向 UP
                    robot.keyPress(KeyEvent.VK_UP); 
                    // robot.keyRelease(KeyEvent.VK_UP);
                    keyDown[num] = KeyEvent.VK_UP;
                    num++;
                    break;


                default:
                    break;
            }
        }


         /**抬起按键 最先按下的最后抬起，*/
        // System.out.println("num =  "+ num);
        
        for(int i=num;i>0; i--){     
            if(keyDown[i] != 0){
                // System.out.println("按下的键  "+ keyDown[i]);
                robot.keyRelease(keyDown[i]);
                
            }           
        }

    
    }

    private void mouse(Robot robot, byte data[]){
        
        Point point = MouseInfo.getPointerInfo().getLocation();//获得鼠标当前位置
        robot.mouseMove(point.x + data[1] , point.y + data[2]);
            /**byte 0 字节内容*/
            StringBuffer bitString = new StringBuffer();
            bitString.append((data[0]>>7)&0x1)
                     .append((data[0]>>6)&0x1)
                     .append((data[0]>>5)&0x1)
                     .append((data[0]>>4)&0x1)
                     .append((data[0]>>3)&0x1)
                     .append((data[0]>>2)&0x1)
                     .append((data[0]>>1)&0x1)
                     .append((data[0]>>0)&0x1);
            // System.out.println("  "+ bitString.toString());   
            if(bitString.toString().charAt(7) == '1'){
                if (MouseButton.getLeft()==false) {
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    MouseButton.setLeftDown();
                    // System.out.println("鼠标左键down"); 
                }
                     
            }
            if(bitString.toString().charAt(7) == '0'){
                if (MouseButton.getLeft()) {
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    MouseButton.setLeftUp();
                    // System.out.println("鼠标左键up");
                }
                
            }
           
            if(bitString.toString().charAt(6) == '1'){
                if (MouseButton.getRight() == false) {
                    robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                    MouseButton.setRightDown();
                    // System.out.println("鼠标右键 down");
                }
                
            }
            if(bitString.toString().charAt(6) == '0'){
                if (MouseButton.getRight()) {
                    robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                    MouseButton.setRightUp();
                    // System.out.println("鼠标右键 up");     
                }      
            }    

            /**
             * 鼠标滚轮
             * 负值表示向上移动/远离用户
             * 正值表示向下移动/朝向用户
             * 
             * 与硬件相反
             * */
            if(data[3]>0){
                robot.mouseWheel( (~(data[3] - 1)));
            }else if(data[3]< 0){
        
                robot.mouseWheel(~(data[3] - 1));
            }
            
          



      


    }





}
