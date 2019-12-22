import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.Timer;

import javax.sound.sampled.*;
import javax.swing.*;

//Player 객체 정보
class Player{
	int imageIndex=0;
	int sizeX; //캐릭터 가로 크기 
	int sizeY; //캐릭터 세로 크기
	int posX;  //좌표  X
	int posY;  //좌표  Y
	boolean moveUp = false;
	boolean moveDown = false;
	int life;  //생명력
	boolean toRight; //left or right player 구분 (공격방향)
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
			b.angle = 330; //시계방향 인가 봄
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

//텔레포트 객체 정보
class Teleport {
	boolean tpAlive = false; // 쓰레드 너무 많이 안 만드려고 teleport skill이 여기에 동작하도록 만듦.
	int tpX; //teleport x좌표
	int tpY; //teleport y좌표
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

//무기(화살표) 방향
class Arrow {
	int arrowNumber = 0;
}

//무기(화살표)방향을 자동으로 회전 및 텔레포트 유지시간 자동 계산 스레드
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
	
	//무기(화살표)방향 및 텔레포트 계산
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
			
			//Left Player 무기(화살표)방향 및 텔레포트 계산
			calc(a1, tp1);
			//Right Player 무기(화살표)방향 및 텔레포트 계산
			calc(a2, tp2);
			panel.repaint();
			
			//무기(화살표)방향이 위->중간->아래->중간->위 순서로 순환
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
		//위/아래 이동 키 처리 
		if(keyCode == this.upKey) {
				p.moveUp = true;
		} else if(keyCode == this.downKey) {
				p.moveDown = true;
		}
		
		//이렇게 분기 안해주면 두개의 리스너에서 각각 실행시켜서 두번 호출되므로 플레이어가 버벅댐
		if (p.moveUp || p.moveDown) { 
			movePlayer(p);
		}
		
		//폭탄이 죽어있을때만(밖으로 나가거나 상대 맞혔을때만) 다시 쏠 수 있도록
		if(keyCode == this.bombKey) { 
			//폭탄은 동시에 최대 5개 허용
	    	if (p.bombs.size() < 5) {
				Bomb b = new Bomb(10, 48, 40, arrow.arrowNumber);
				b.alive = true;
				p.addBomb(b);
				a.loadAudio(bombSound);
	    	}
		}
		
		//텔레포트 수행 (기존 텔레포트 수행중에는 사용 불가) 
		if(keyCode == this.teleportKey && tp.tpAlive == false) {
			tp.setPos(p.posX, p.posY);
			teleportPlayer(p);
			a.loadAudio(tpSound);
		}
	}
	
	public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();
		//위/아래 이동 키 처리 
		if(keyCode == upKey) {
				p.moveUp = false;
		} else if(keyCode == downKey) {
				p.moveDown = false;
		}
	}

	//패널 범위 내에서 플레이어 이동
	private void movePlayer(Player p) {
		//플레이어 위로 이동  
		if(p.moveUp == true) {
			p.posY -= FLYING_UNIT;
			if(p.posY < 0)
				p.posY = 0;
		}

		//플레이어 아래로이동  
		if(p.moveDown == true) {
			p.posY += FLYING_UNIT;
			if(p.posY > 400)
				p.posY = 400;
		}
		panel.repaint();
	}
	
	//패널 범위 내에서 텔레포트 수행
	private void teleportPlayer(Player p) {
		//플레이어 위로 텔레포트 수행  
		if(p.moveUp == true) {
			p.posY -= 150;
			if(p.posY < 0)
				p.posY = 0;
		}
		//플레이어 아래로 텔레포트 수행  
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

//폭탄 정보
class Bomb {
	int arrowNumber; //폭탄 방향(발사시 무기 방향과 동일)
	int angle; //폭탄 발사각도
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

//폭탄 만들고, 피격 계산 스레드
class BombRunnable implements Runnable {
	JPanel panel;
	Player p1;//캐릭터 크기 73x93
	Player p2;
	boolean alive = true;
	public BombRunnable(JPanel panel,Player p1, Player p2) {
		this.panel = panel;
		this.p1 = p1;
		this.p2 = p2;
	}
	
	//attacker플레이어의 bomb이 enemy를 맞췄는지 계산, toRight는 폭탄 진행방향
	private void calcBomb(Player attacker, Player enemy, boolean toRight) {
		//발사된 모든 폭탄에 대해 계산
		for (Bomb b : attacker.bombs) {
			//폭탄  X축 계산
			if (toRight) //오른쪽으로 발사
				b.posX += Math.cos(Math.toRadians(b.angle)) * b.speed;
			else //왼쪽으로 발사
				b.posX -= Math.cos(Math.toRadians(b.angle)) * b.speed;
			
			//폭탄  Y축 계산
			b.posY += Math.sin(Math.toRadians(b.angle)) * b.speed;
			panel.repaint();
			
			//폭탄이 패널 밖으로 나가면 사라짐
			if(b.posX < 0 || b.posX >= 500 || b.posY < 0 || b.posY >= 600) {
				b.alive = false;
				continue;
			}
	
			//피격판정
			double distance = Math.sqrt(((double)enemy.posX-b.posX)*(enemy.posX-b.posX) 
					+ (enemy.posY-b.posY)*(enemy.posY-b.posY));
			
			//캐릭터 Y축이 사이즈 더 커서 피격판정 좋게 하기위해 emeny.sizeY사용
			if(b.alive == true && distance <= ((double)(enemy.sizeY)/2) + (b.sizeX)/2) {
				enemy.life -= 30;
				b.alive = false;
				//System.out.println(enemy+ "hitted!");
				panel.repaint();
				continue;
			}
		}
		//패널 밖으로 나가거나 상대를 맞춘 폭탄 제거, bombs(Vector) 수정시 Main 스레드에서 사용하는 것을 방지(동기화)
		//동기화 미 수행시 여기서 벡터내의 객체 삭제할 때 다른 스레드에서 사용하다가 에러 발생할 수 있음.
	    synchronized(attacker.bombs){
			for (int i = attacker.bombs.size()-1; i >= 0; i--) {
				if (attacker.bombs.get(i).alive == false) {
					attacker.bombs.remove(i);
				}
			}
	    }
	}
	//스레드 종료
	public void kill() {
		this.alive = false;
	}
	
	public void run() {
		while(this.alive) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
			}
			
			//p1이 p2를 공격 계산
			calcBomb(p1, p2, true);
			
			//p2가 p1을 공격 계산
			calcBomb(p2, p1, false); 
		}
	}
}

//효과음 수행(기말고사때 올려주신 코드 응용)
class Audio {
	private Clip clip;
	JFrame frame;
	public Audio(JFrame frame) {
		this.frame = frame;
	}

	public void loadAudio(String pathName) {
		try {
			File audioFile = new File(pathName); // 오디오 파일의 경로명
			final AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile); // 오디오 파일로부터
			clip = AudioSystem.getClip(); // 비어있는 오디오 클립 만들기

			clip.addLineListener(new LineListener() {
				public void update(LineEvent e) {
					if (e.getType() == LineEvent.Type.STOP) { // clip.stop()이 호출되거나 재생이 끝났을 때
						try {
							audioStream.close();
						} catch (IOException e1) {
							e1.printStackTrace();		
						}
	                }
				}
            });
			clip.open(audioStream); // 재생할 오디오 스트림 열기
			clip.start(); // 재생 시작

		}
		catch (LineUnavailableException e) { e.printStackTrace(); }
		catch (UnsupportedAudioFileException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
	}
	public void loadAudio_loop(String pathName) {
		try {
			File audioFile = new File(pathName); // 오디오 파일의 경로명
			final AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile); // 오디오 파일로부터
			clip = AudioSystem.getClip(); // 비어있는 오디오 클립 만들기

			clip.addLineListener(new LineListener() {
				public void update(LineEvent e) {
					if (e.getType() == LineEvent.Type.STOP) { // clip.stop()이 호출되거나 재생이 끝났을 때
						try {
							audioStream.close();
						} catch (IOException e1) {
							e1.printStackTrace();		
						}
	                }
				}
            });
			clip.open(audioStream); // 재생할 오디오 스트림 열기
			clip.start(); // 재생 시작
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
	private String bgSound="sounds/bg.wav"; //배경효과음
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
		//배경음 시작
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
	Arrow a1 = new Arrow(); //p1 폭탄 방향
	Arrow a2 = new Arrow(); //p2 폭탄 방향
	Player p1 = new Player(15, 200, 70, 73, 93, true);
	Player p2 = new Player(400, 200, 70, 73, 93, false);
	private MyPanel panel = new MyPanel();
	Teleport tp1 = new Teleport(p1); //p1의 텔레포트
	Teleport tp2 = new Teleport(p2); //p2의 텔레포트
	
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
	private ImageIcon bomb_icon = new ImageIcon("images/bomb.png"); //폭탄 이미지
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
	private String tpSound="sounds/teleport.wav"; //텔레포트 효과음
	private String deadSound="sounds/die.wav";
	private String bgSound="sounds/bg.wav"; //배경효과음
	private String announceSound = "sounds/announce.wav"; //아나운서 효과음
	private JLabel b_audioLabel=new JLabel(bombSound);
	private JLabel t_audioLabel=new JLabel(tpSound);
	private JLabel d_audioLabel=new JLabel(deadSound);
	private boolean closedByX = false;//창  닫은 경우, draw 가 아닌 경우임을 명시하는 flag
	
	public Dodger() {
		//Main Frame 설정
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
		this.addWindowListener(new WindowAdapter(){	//X표로 창 닫을때 isAlive가 true로 남음
            public void windowClosing(WindowEvent e) { 
				ar.kill();	br.kill();	closedByX = true;
            }
		});

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
				
		//무기(화살표) 이미지 설정
		arrow_img1 = new Image[4];
		arrow_img2 = new Image[4];
		for(int i=0; i<arrow_icon1.length; i++) {
			arrow_img1[i] = arrow_icon1[i].getImage();
			arrow_img2[i] = arrow_icon2[i].getImage();
		}

		//플레이어 이미지 설정
		p1_img = new Image[4];
		p2_img = new Image[4];
		for(int i=0; i<p1_icon.length; i++) {
			p1_img[i] = p1_icon[i].getImage();
			p2_img[i] = p2_icon[i].getImage();
		}

		//오디오 라벨 설정
		panel.add(b_audioLabel);
		panel.add(t_audioLabel);
		panel.add(d_audioLabel);
		
		//Arrow/Bomb 스레드 시작
		ath.start();
		bth.start();
		
		//아나운서 효과음 시작
		audio.loadAudio(announceSound);
		
	}
	
	class MyPanel extends JPanel{
		private void drawPlayer(Graphics g, Player p, Image[] p_img) {
			//최대 체력바 그리기
			g.setColor(Color.BLACK);
			g.drawRect(p.posX, p.posY+100, 70, 15);
			
			
			if(p.life >= 0) {
				//현재 체력바 그리기
				g.setColor(Color.GREEN);
				g.fillRect(p.posX, p.posY+100, p.life, 15);
				
				//이동하는 플레이어 그리기
				if(p.moveDown || p.moveUp) {
					//쓰레드로 인해 repaint가 여러번되는거같아서 imgCount에 /2하여 다른 이미지 출력하는데에 딜레이줌.
					g.drawImage(p_img[p.imageIndex/2], p.posX, p.posY, p.sizeX, p.sizeY, this); // 캐릭터 크기 73x93
					p.imageIndex++;
					if(p.imageIndex >= 6)
						p.imageIndex = 0;
				}
				else { //정지상태 플레이어 그리기
					g.drawImage(p_img[p.imageIndex/2], p.posX, p.posY, p.sizeX, p.sizeY, this); // 캐릭터 크기 73x93
				}
			}			
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(bg_img, 0, 0, 500, 600, this);

			//플레이어 그리기
			drawPlayer(g, p1, p1_img);
			drawPlayer(g, p2, p2_img);
		
			//폭탄 방향(화살표) 그리기
			g.drawImage(arrow_img1[ar.a1.arrowNumber], p1.posX+50, p1.posY, this);
			g.drawImage(arrow_img2[ar.a2.arrowNumber], p2.posX-70, p2.posY, this);

			//폭탄 그리기 : BombRunnable 쓰레드와의 충돌 방지 (BombRunnable에서는 벡터 내 객체 삭제)
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

		    //텔레포트 그리기
		    if(tp1.tpAlive == true) {
				g.drawImage(tp_img, tp1.tpX, tp1.tpY, tp1.p.sizeX, tp1.p.sizeY, this);
			}
			if(tp2.tpAlive == true) {
				g.drawImage(tp_img, tp2.tpX, tp2.tpY, tp2.p.sizeX, tp2.p.sizeY, this);
			}
			
			//플레이어 죽었을 때 이미지 그리기
			if(p1.life <= 0) {
				g.drawImage(dead_img, p1.posX, p1.posY, p1.sizeX, p1.sizeY, this);
				//스레드 모두 종료(ArrowRunnable, BombRunnable)
				ar.kill();
				br.kill();
			}
			if(p2.life <= 0) {
				g.drawImage(dead_img, p2.posX, p2.posY, p2.sizeX, p2.sizeY, this);
				//스레드 모두 종료(ArrowRunnable, BombRunnable)
				ar.kill();
				br.kill();
			}
		}
		
		//Arrow 및 Bomb 스레드 상태 확인
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
					//Arrow / Bomb 스레드 종료 대기
					while (g.panel.isAlive()) {
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
						}
					}
					if(!g.closedByX) {	//창 닫은 경우 제외
						//게임 종료 처리 
						g.audio.loadAudio(g.deadSound);
						if (g.p1.life > g.p2.life)
							JOptionPane.showMessageDialog(g.panel, "Left Player win!!");
						else if (g.p1.life < g.p2.life) 
							JOptionPane.showMessageDialog(g.panel, "Right Player win!!");
						else
							JOptionPane.showMessageDialog(g.panel, "Draw!!");
						
						g.dispose();	//해당 JPanel 하나만 종료
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

