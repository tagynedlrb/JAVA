import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.Timer;

import javax.sound.sampled.*;
import javax.swing.*;

//Player ��ü ����
class Player{
	int imageIndex=0;
	int sizeX; //ĳ���� ���� ũ�� 
	int sizeY; //ĳ���� ���� ũ��
	int posX;  //��ǥ  X
	int posY;  //��ǥ  Y
	boolean moveUp = false;
	boolean moveDown = false;
	int life;  //�����
	boolean toRight; //left or right player ���� (���ݹ���)
	Vector<Bomb> bombs = new Vector<Bomb>();
	public Player(int posX, int posY, int life, int sizeX, int sizeY, boolean toRight) {
		this.posX = posX;
		this.posY = posY;
		this.life = life;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.toRight = toRight;
	}
	public void addBomb(Bomb b) {
		if(b.arrowNumber == 0)
			b.angle = 330; //�ð���� �ΰ� ��
		else if(b.arrowNumber == 1 || b.arrowNumber == 3)
			b.angle = 0;
		else if(b.arrowNumber == 2)
			b.angle = 30;
		if(toRight) {
			b.posX = this.posX+50;
		} else {
			b.posX = this.posX-40;
		}

		b.posY = this.posY+30;
	
	    synchronized(this.bombs){
    		this.bombs.add(b);
	    }
	}
}

//�ڷ���Ʈ ��ü ����
class Teleport {
	boolean tpAlive = false; // ������ �ʹ� ���� �� ������� teleport skill�� ���⿡ �����ϵ��� ����.
	int tpX; //teleport x��ǥ
	int tpY; //teleport y��ǥ
	Player p;
	int tpTime = 0;
	public Teleport(Player p) {
		this.p = p;
	}
	public void setPos(int tpX, int tpY) {
		this.tpX = tpX;
		this.tpY = tpY;
	}
}

//����(ȭ��ǥ) ����
class Arrow {
	int arrowNumber = 0;
}

//����(ȭ��ǥ)������ �ڵ����� ȸ�� �� �ڷ���Ʈ �����ð� �ڵ� ��� ������
class ArrowRunnable implements Runnable {
	JPanel panel;
	Arrow a1,a2;
	Teleport tp1,tp2;
	boolean alive=true;
	
	public ArrowRunnable(JPanel panel, Arrow a1, Teleport tp1, Arrow a2, Teleport tp2) {
		this.panel = panel;
		this.a1 = a1;
		this.tp1 = tp1;
		this.a2 = a2;
		this.tp2 = tp2;
	}
	
	//����(ȭ��ǥ)���� �� �ڷ���Ʈ ���
	private void calc(Arrow a, Teleport tp) {
		a.arrowNumber++;
		if(tp.tpAlive) {
			tp.tpTime++;
			if(tp.tpTime >= 2) {
				tp.tpAlive = false;
				tp.tpTime = 0;
			}
		}
	}
	
	public void kill() {
		this.alive = false;
	}
	
	@Override
	public void run() {
		while(this.alive) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			
			//Left Player ����(ȭ��ǥ)���� �� �ڷ���Ʈ ���
			calc(a1, tp1);
			//Right Player ����(ȭ��ǥ)���� �� �ڷ���Ʈ ���
			calc(a2, tp2);
			panel.repaint();
			
			//����(ȭ��ǥ)������ ��->�߰�->�Ʒ�->�߰�->�� ������ ��ȯ
			if(a1.arrowNumber >= 4) {
				a1.arrowNumber = 0;
			}
			if(a2.arrowNumber >= 4) {
				a2.arrowNumber = 0;
			}
		}
	}
}

class MyKeyListener extends KeyAdapter {
	JPanel panel;
	Player p;
	Player emy;
	int upKey, downKey;
	int bombKey;
	int multiBombKey;
	int teleportKey;
	Audio a;
	String bombSound;
	String tpSound;
	Teleport tp;
	BombRunnable br;
	Arrow arrow;
	private final int FLYING_UNIT = 8;
	public MyKeyListener(JPanel panel, Player p, Player emy, int upKey, int downKey, int bombKey, int teleportKey,
			Audio a, String bombSound, String tpSound, Teleport tp, BombRunnable br, Arrow arrow) {
		this.panel = panel;
		this.p = p;
		this.emy = emy;
		this.upKey = upKey;
		this.downKey = downKey;
		this.bombKey = bombKey;
		this.teleportKey = teleportKey;
		this.a = a;
		this.bombSound = bombSound;
		this.tpSound = tpSound;
		this.tp = tp;
		this.br = br;
		this.arrow = arrow;
	}
	
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		//��/�Ʒ� �̵� Ű ó�� 
		if(keyCode == this.upKey) {
				p.moveUp = true;
		} else if(keyCode == this.downKey) {
				p.moveDown = true;
		}
		
		//�̷��� �б� �����ָ� �ΰ��� �����ʿ��� ���� ������Ѽ� �ι� ȣ��ǹǷ� �÷��̾ ������
		if (p.moveUp || p.moveDown) { 
			movePlayer(p);
		}
		
		//��ź�� �׾���������(������ �����ų� ��� ����������) �ٽ� �� �� �ֵ���
		if(keyCode == this.bombKey) { 
			//��ź�� ���ÿ� �ִ� 5�� ���
	    	if (p.bombs.size() < 5) {
				Bomb b = new Bomb(10, 48, 40, arrow.arrowNumber);
				b.alive = true;
				p.addBomb(b);
				a.loadAudio(bombSound);
	    	}
		}
		
		//�ڷ���Ʈ ���� (���� �ڷ���Ʈ �����߿��� ��� �Ұ�) 
		if(keyCode == this.teleportKey && tp.tpAlive == false) {
			tp.setPos(p.posX, p.posY);
			teleportPlayer(p);
			a.loadAudio(tpSound);
		}
	}
	
	public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();
		//��/�Ʒ� �̵� Ű ó�� 
		if(keyCode == upKey) {
				p.moveUp = false;
		} else if(keyCode == downKey) {
				p.moveDown = false;
		}
	}

	//�г� ���� ������ �÷��̾� �̵�
	private void movePlayer(Player p) {
		//�÷��̾� ���� �̵�  
		if(p.moveUp == true) {
			p.posY -= FLYING_UNIT;
			if(p.posY < 0)
				p.posY = 0;
		}

		//�÷��̾� �Ʒ����̵�  
		if(p.moveDown == true) {
			p.posY += FLYING_UNIT;
			if(p.posY > 400)
				p.posY = 400;
		}
		panel.repaint();
	}
	
	//�г� ���� ������ �ڷ���Ʈ ����
	private void teleportPlayer(Player p) {
		//�÷��̾� ���� �ڷ���Ʈ ����  
		if(p.moveUp == true) {
			p.posY -= 150;
			if(p.posY < 0)
				p.posY = 0;
		}
		//�÷��̾� �Ʒ��� �ڷ���Ʈ ����  
		else if(p.moveDown == true) {
			p.posY += 150;
			if(p.posY > 400)
				p.posY = 400;
		}
		tp.tpAlive = true;
		tp.tpTime = 0;
		panel.repaint();
	}
}

//��ź ����
class Bomb {
	int arrowNumber; //��ź ����(�߻�� ���� ����� ����)
	int angle; //��ź �߻簢��
	int posX;
	int posY;
	int speed;
	int sizeX;
	int sizeY;
	boolean alive=false;
	public Bomb(int speed, int sizeX, int sizeY, int arrowNumber) {
		this.arrowNumber = arrowNumber;
		this.speed = speed;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
	}
}

//��ź �����, �ǰ� ��� ������
class BombRunnable implements Runnable {
	JPanel panel;
	Player p1;//ĳ���� ũ�� 73x93
	Player p2;
	boolean alive = true;
	public BombRunnable(JPanel panel,Player p1, Player p2) {
		this.panel = panel;
		this.p1 = p1;
		this.p2 = p2;
	}
	
	//attacker�÷��̾��� bomb�� enemy�� ������� ���, toRight�� ��ź �������
	private void calcBomb(Player attacker, Player enemy, boolean toRight) {
		//�߻�� ��� ��ź�� ���� ���
		for (Bomb b : attacker.bombs) {
			//��ź  X�� ���
			if (toRight) //���������� �߻�
				b.posX += Math.cos(Math.toRadians(b.angle)) * b.speed;
			else //�������� �߻�
				b.posX -= Math.cos(Math.toRadians(b.angle)) * b.speed;
			
			//��ź  Y�� ���
			b.posY += Math.sin(Math.toRadians(b.angle)) * b.speed;
			panel.repaint();
			
			//��ź�� �г� ������ ������ �����
			if(b.posX < 0 || b.posX >= 500 || b.posY < 0 || b.posY >= 600) {
				b.alive = false;
				continue;
			}
	
			//�ǰ�����
			double distance = Math.sqrt(((double)enemy.posX-b.posX)*(enemy.posX-b.posX) 
					+ (enemy.posY-b.posY)*(enemy.posY-b.posY));
			
			//ĳ���� Y���� ������ �� Ŀ�� �ǰ����� ���� �ϱ����� emeny.sizeY���
			if(b.alive == true && distance <= ((double)(enemy.sizeY)/2) + (b.sizeX)/2) {
				enemy.life -= 30;
				b.alive = false;
				//System.out.println(enemy+ "hitted!");
				panel.repaint();
				continue;
			}
		}
		//�г� ������ �����ų� ��븦 ���� ��ź ����, bombs(Vector) ������ Main �����忡�� ����ϴ� ���� ����(����ȭ)
		//����ȭ �� ����� ���⼭ ���ͳ��� ��ü ������ �� �ٸ� �����忡�� ����ϴٰ� ���� �߻��� �� ����.
	    synchronized(attacker.bombs){
			for (int i = attacker.bombs.size()-1; i >= 0; i--) {
				if (attacker.bombs.get(i).alive == false) {
					attacker.bombs.remove(i);
				}
			}
	    }
	}
	//������ ����
	public void kill() {
		this.alive = false;
	}
	
	public void run() {
		while(this.alive) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
			}
			
			//p1�� p2�� ���� ���
			calcBomb(p1, p2, true);
			
			//p2�� p1�� ���� ���
			calcBomb(p2, p1, false); 
		}
	}
}

//ȿ���� ����(�⸻��綧 �÷��ֽ� �ڵ� ����)
class Audio {
	private Clip clip;
	JFrame frame;
	public Audio(JFrame frame) {
		this.frame = frame;
	}

	public void loadAudio(String pathName) {
		try {
			File audioFile = new File(pathName); // ����� ������ ��θ�
			final AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile); // ����� ���Ϸκ���
			clip = AudioSystem.getClip(); // ����ִ� ����� Ŭ�� �����

			clip.addLineListener(new LineListener() {
				public void update(LineEvent e) {
					if (e.getType() == LineEvent.Type.STOP) { // clip.stop()�� ȣ��ǰų� ����� ������ ��
						try {
							audioStream.close();
						} catch (IOException e1) {
							e1.printStackTrace();		
						}
	                }
				}
            });
			clip.open(audioStream); // ����� ����� ��Ʈ�� ����
			clip.start(); // ��� ����

		}
		catch (LineUnavailableException e) { e.printStackTrace(); }
		catch (UnsupportedAudioFileException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
	}
	public void loadAudio_loop(String pathName) {
		try {
			File audioFile = new File(pathName); // ����� ������ ��θ�
			final AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile); // ����� ���Ϸκ���
			clip = AudioSystem.getClip(); // ����ִ� ����� Ŭ�� �����

			clip.addLineListener(new LineListener() {
				public void update(LineEvent e) {
					if (e.getType() == LineEvent.Type.STOP) { // clip.stop()�� ȣ��ǰų� ����� ������ ��
						try {
							audioStream.close();
						} catch (IOException e1) {
							e1.printStackTrace();		
						}
	                }
				}
            });
			clip.open(audioStream); // ����� ����� ��Ʈ�� ����
			clip.start(); // ��� ����
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		}
		catch (LineUnavailableException e) { e.printStackTrace(); }
		catch (UnsupportedAudioFileException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
	}
}

class DodgerUI extends JFrame{
	private MyPanel panel = new MyPanel();
	Audio audio = new Audio(this);
	private String bgSound="sounds/bg.wav"; //���ȿ����
	private ImageIcon icon = new ImageIcon("images/main.png");
	private Image img = icon.getImage();
	private JButton start = new JButton("Start!");
	int flag = 0;
	public DodgerUI() {
		
		setTitle("Ezreal Mirror UI!!!");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setContentPane(panel);
		panel.setLayout(null);
		start.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					flag = 1;
				}
			});
		start.setBounds(180, 350, 130, 50);
		panel.add(start);
		
		
		setFocusable(true);
		requestFocus();
		setSize(500,600);
		setVisible(true);

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);	
		//����� ����
		audio.loadAudio_loop(bgSound);
	}
	class MyPanel extends JPanel{
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(img, 40,20,400,250,this);
			g.setColor(Color.BLACK);
			g.setFont(new Font("Arial", Font.ITALIC, 40));
			g.drawString("Ezreal Mirror Battle", 70, 320);
		}
		
	}
}

//Main Frame
public class Dodger extends JFrame {
	Arrow a1 = new Arrow(); //p1 ��ź ����
	Arrow a2 = new Arrow(); //p2 ��ź ����
	Player p1 = new Player(15, 200, 70, 73, 93, true);
	Player p2 = new Player(400, 200, 70, 73, 93, false);
	private MyPanel panel = new MyPanel();
	Teleport tp1 = new Teleport(p1); //p1�� �ڷ���Ʈ
	Teleport tp2 = new Teleport(p2); //p2�� �ڷ���Ʈ
	
	ArrowRunnable ar = new ArrowRunnable(panel, a1, tp1, a2, tp2);
	Thread ath = new Thread(ar);
	BombRunnable br = new BombRunnable(panel, p1, p2);
	Thread bth = new Thread(br);
	
	private ImageIcon[] p1_icon = {new ImageIcon("images/p1_1.png"), new ImageIcon("images/p1_2.png"), new ImageIcon("images/p1_3.png")};
	private ImageIcon[] p2_icon = {new ImageIcon("images/p2_1.png"), new ImageIcon("images/p2_2.png"), new ImageIcon("images/p2_3.png")};
	private ImageIcon[] arrow_icon1 = {new ImageIcon("images/arrowUp30.png"), new ImageIcon("images/arrowStraight.png"),
			new ImageIcon("images/arrowDown30.png"), new ImageIcon("images/arrowStraight.png")};
	private ImageIcon[] arrow_icon2 = {new ImageIcon("images/LeftarrowUp30.png"), new ImageIcon("images/LeftarrowStraight.png"),
			new ImageIcon("images/LeftarrowDown30.png"), new ImageIcon("images/LeftarrowStraight.png")};
	private ImageIcon bomb_icon = new ImageIcon("images/bomb.png"); //��ź �̹���
	private ImageIcon bg_icon = new ImageIcon("images/bg2.png");
	private ImageIcon tp_icon = new ImageIcon("images/teleport.png");
	private ImageIcon dead_icon = new ImageIcon("images/dead.png");
	
	private Image[] p1_img;
	private Image[] p2_img;
	private Image[] arrow_img1;
	private Image[] arrow_img2;
	private Image bomb_img = bomb_icon.getImage();
	private Image bg_img = bg_icon.getImage();
	private Image tp_img = tp_icon.getImage();
	private Image dead_img = dead_icon.getImage();
	
	Audio audio = new Audio(this);
	private String bombSound="sounds/bomb.wav";
	private String tpSound="sounds/teleport.wav"; //�ڷ���Ʈ ȿ����
	private String deadSound="sounds/die.wav";
	private String bgSound="sounds/bg.wav"; //���ȿ����
	private String announceSound = "sounds/announce.wav"; //�Ƴ�� ȿ����
	private JLabel b_audioLabel=new JLabel(bombSound);
	private JLabel t_audioLabel=new JLabel(tpSound);
	private JLabel d_audioLabel=new JLabel(deadSound);
	private boolean closedByX = false;//â  ���� ���, draw �� �ƴ� ������� ����ϴ� flag
	
	public Dodger() {
		//Main Frame ����
		setTitle("Dodger!!!");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setContentPane(panel);
		panel.setLayout(null);
		panel.addKeyListener(new MyKeyListener(panel, p1, p2, KeyEvent.VK_W, KeyEvent.VK_S, 
				KeyEvent.VK_F, KeyEvent.VK_G, audio, bombSound, tpSound, tp1, br, a1));
		panel.addKeyListener(new MyKeyListener(panel, p2, p1, KeyEvent.VK_UP, KeyEvent.VK_DOWN, 
				KeyEvent.VK_PERIOD, KeyEvent.VK_SLASH, audio, bombSound, tpSound, tp2, br, a1));
		panel.setFocusable(true);
		panel.requestFocus();
		setSize(500,600);
		setVisible(true);
		this.addWindowListener(new WindowAdapter(){	//Xǥ�� â ������ isAlive�� true�� ����
            public void windowClosing(WindowEvent e) { 
				ar.kill();	br.kill();	closedByX = true;
            }
		});

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
				
		//����(ȭ��ǥ) �̹��� ����
		arrow_img1 = new Image[4];
		arrow_img2 = new Image[4];
		for(int i=0; i<arrow_icon1.length; i++) {
			arrow_img1[i] = arrow_icon1[i].getImage();
			arrow_img2[i] = arrow_icon2[i].getImage();
		}

		//�÷��̾� �̹��� ����
		p1_img = new Image[4];
		p2_img = new Image[4];
		for(int i=0; i<p1_icon.length; i++) {
			p1_img[i] = p1_icon[i].getImage();
			p2_img[i] = p2_icon[i].getImage();
		}

		//����� �� ����
		panel.add(b_audioLabel);
		panel.add(t_audioLabel);
		panel.add(d_audioLabel);
		
		//Arrow/Bomb ������ ����
		ath.start();
		bth.start();
		
		//�Ƴ�� ȿ���� ����
		audio.loadAudio(announceSound);
		
	}
	
	class MyPanel extends JPanel{
		private void drawPlayer(Graphics g, Player p, Image[] p_img) {
			//�ִ� ü�¹� �׸���
			g.setColor(Color.BLACK);
			g.drawRect(p.posX, p.posY+100, 70, 15);
			
			
			if(p.life >= 0) {
				//���� ü�¹� �׸���
				g.setColor(Color.GREEN);
				g.fillRect(p.posX, p.posY+100, p.life, 15);
				
				//�̵��ϴ� �÷��̾� �׸���
				if(p.moveDown || p.moveUp) {
					//������� ���� repaint�� �������Ǵ°Ű��Ƽ� imgCount�� /2�Ͽ� �ٸ� �̹��� ����ϴµ��� ��������.
					g.drawImage(p_img[p.imageIndex/2], p.posX, p.posY, p.sizeX, p.sizeY, this); // ĳ���� ũ�� 73x93
					p.imageIndex++;
					if(p.imageIndex >= 6)
						p.imageIndex = 0;
				}
				else { //�������� �÷��̾� �׸���
					g.drawImage(p_img[p.imageIndex/2], p.posX, p.posY, p.sizeX, p.sizeY, this); // ĳ���� ũ�� 73x93
				}
			}			
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(bg_img, 0, 0, 500, 600, this);

			//�÷��̾� �׸���
			drawPlayer(g, p1, p1_img);
			drawPlayer(g, p2, p2_img);
		
			//��ź ����(ȭ��ǥ) �׸���
			g.drawImage(arrow_img1[ar.a1.arrowNumber], p1.posX+50, p1.posY, this);
			g.drawImage(arrow_img2[ar.a2.arrowNumber], p2.posX-70, p2.posY, this);

			//��ź �׸��� : BombRunnable ��������� �浹 ���� (BombRunnable������ ���� �� ��ü ����)
		    synchronized(p1.bombs){
				for (Bomb b : p1.bombs) {
					if(b.alive == true) 
						g.drawImage(bomb_img, b.posX, b.posY, this);
				}
		    }
		    synchronized(p2.bombs){
				for (Bomb b : p2.bombs) {
					if(b.alive == true) 
						g.drawImage(bomb_img, b.posX, b.posY, this);
				}
		    }

		    //�ڷ���Ʈ �׸���
		    if(tp1.tpAlive == true) {
				g.drawImage(tp_img, tp1.tpX, tp1.tpY, tp1.p.sizeX, tp1.p.sizeY, this);
			}
			if(tp2.tpAlive == true) {
				g.drawImage(tp_img, tp2.tpX, tp2.tpY, tp2.p.sizeX, tp2.p.sizeY, this);
			}
			
			//�÷��̾� �׾��� �� �̹��� �׸���
			if(p1.life <= 0) {
				g.drawImage(dead_img, p1.posX, p1.posY, p1.sizeX, p1.sizeY, this);
				//������ ��� ����(ArrowRunnable, BombRunnable)
				ar.kill();
				br.kill();
			}
			if(p2.life <= 0) {
				g.drawImage(dead_img, p2.posX, p2.posY, p2.sizeX, p2.sizeY, this);
				//������ ��� ����(ArrowRunnable, BombRunnable)
				ar.kill();
				br.kill();
			}
		}
		
		//Arrow �� Bomb ������ ���� Ȯ��
		public boolean isAlive() {
			return ar.alive && br.alive;
		}
	}

	public static void main(String [] args) {
		DodgerUI UI = new DodgerUI();
		Timer timer = new Timer();
		TimerTask task= new TimerTask() {
			@Override
			public void run() {
				if(UI.flag == 1){
					UI.flag = 0;
					Dodger g = new Dodger();
					//Arrow / Bomb ������ ���� ���
					while (g.panel.isAlive()) {
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
						}
					}
					if(!g.closedByX) {	//â ���� ��� ����
						//���� ���� ó�� 
						g.audio.loadAudio(g.deadSound);
						if (g.p1.life > g.p2.life)
							JOptionPane.showMessageDialog(g.panel, "Left Player win!!");
						else if (g.p1.life < g.p2.life) 
							JOptionPane.showMessageDialog(g.panel, "Right Player win!!");
						else
							JOptionPane.showMessageDialog(g.panel, "Draw!!");
						
						g.dispose();	//�ش� JPanel �ϳ��� ����
					}

				}	
			}
		};
		
		timer.scheduleAtFixedRate(task, 1000, 1000);  
		 try {
	         Thread.sleep(200000);
	      } catch(InterruptedException ex) {
	         //
	      }
	      task.cancel();
	
	}
}

