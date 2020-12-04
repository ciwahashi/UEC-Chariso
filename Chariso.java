

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.awt.geom.*; /* アフィン変換を利用するために必要 */


////////////////////////////////////////////////////////////
/* プログラム内で使用する変数をマクロで定義 */

class Macro{
    /* 画面のサイズ（縦横の最大値） */
    public static final int max = 500;

    /* 地面のサイズ（幅） */
    public static final int gw = max + 100;

    /* 地面のサイズ（高さ） */
    public static final int gh = max / 5 * 3;

    /* ジャンプの高さ */
    public static final int jh = max / 6;

    /* 自転車のサイズ */
    public static final int bs = 20;

    /* メインのタイマーの遅延時間 */
    public static final int td = 6;
}


//////////////////////////////////////////////////////
/* 長方形を描くためのクラス */

class Rect {
    protected int x, y, w, h;
    protected Color c;

    public Rect(int x, int y, int w, int h, Color c){
        this.x = x; this.y = y;
        this.w = w; this.h = h;
        this.c = c;
    }

    public void draw(Graphics g){
        g.setColor(c);
        g.fillRect(x, y, w, h);
    }
}

//////////////////////////////////////////////////////
/* 地面のクラス */

class Ground extends Rect{
    private int i;

    /* 可変長配列を利用 */
    private ArrayList<Rect> ground;
    private ArrayList<Rect> obstacle;

    public Ground(){
        super(0, 0, 0, 0, Color.black);
        create();
    }

    /* 地面と障害物を作る */
    public void create(){
        ground = new ArrayList<Rect>();
        obstacle = new ArrayList<Rect>();
        for(i = -100; i <= Macro.max; i++){
            ground.add(new Rect(i, Macro.gh, 1, Macro.max-Macro.gh, Color.black));
            obstacle.add(new Rect(i, Macro.max, 1, 0, new Color(115, 66, 41)));
        }
    }

    /* 地面と障害物を1だけ左に動かす */
    public void move(int n){
        for(Rect r : ground) r.x--;
        for(Rect r : obstacle) r.x--;
        ground.get(n).x = Macro.max;
        obstacle.get(n).x = Macro.max;
    }

    /* 地面と障害物を初期化 */
    public void init(int n){
        ground.get(n).y = Macro.gh;
        ground.get(n).h = Macro.max-Macro.gh;
        obstacle.get(n).y = Macro.max;
        obstacle.get(n).h = 0;
    }

    /* 穴を作成 */
    public void create_hole(int n, int w){
        for(i = 0; i < w; i++){

            /* 障害物を初期化 */
            obstacle.get(n+i).y = Macro.max;
            obstacle.get(n+i).h = 0;

            /* 穴を作成 */
            ground.get(n+i).y = Macro.max;
            ground.get(n+i).h = 0;
            if(n+i >= Macro.gw) n = -i-1;

        }
    }

    /* 障害物を作成 */
    public void create_obst(int n, int w, int h){
        for(i = 0; i < w; i++){

            /* 地面を初期化 */
            ground.get(n+i).y = Macro.gh;
            ground.get(n+i).h = Macro.max-Macro.gh;

            /* 障害物を作成 */
            obstacle.get(n+i).y = Macro.gh - h + Math.abs(w/2-i);
            obstacle.get(n+i).h = h /*- Math.abs(w/2-i)*/;
            if(n+i >= Macro.gw) n = -i-1;

        }
    }

    public void draw(Graphics g){
        for(Rect r : obstacle) r.draw(g);
        for(Rect r : ground) r.draw(g);
    }

    /* 地面と障害物のy座標のうち小さいほうを返す（当たり判定に使用）*/
    public int getY(int n){
        if(ground.get(n).y < obstacle.get(n).y) return ground.get(n).y;
        return obstacle.get(n).y;
    }
}


////////////////////////////////////////////////////////
/* 背景 */

class Background{
    private int x = 0, y = 0;

    /* 画像の取り込み */
    private Image ao = Toolkit.getDefaultToolkit().getImage("image/aozora500.png");

    public void draw(Graphics g, JPanel p){
        g.drawImage(ao, x, y, p);
        g.drawImage(ao, x+500, y, p);
        g.drawImage(ao, x+1000, y, p);
    }

    /* 背景を1だけ左に動かす */
    public void xmove(){
        x--;
        if(x == -Macro.max) x = 0;
    }
}


///////////////////////////////////////////////////
/* 走行距離表示のためのクラス */

class Mileage extends JLabel{
    /* 走行距離 */
    private double d = 0;
    private String m = "";

    public Mileage(){
        this.setBounds(400, 0, 100, 50);
        this.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
    }

    /* 走行距離を文字列で返す */
    public String getMile(){
        return String.format("%1$.2f", d);
    }

    /* 走行距離の更新 */
    public void updatem(){
        d += 0.001 * Macro.td;

        /* 桁数を指定して文字列に変換 */
        m = String.format("%1$.2f", d) + " m";
        setText(m);
    }
}


///////////////////////////////////////////////////
/* スピード表示のためのクラス */

class Speed extends JLabel{
    /* スピードを段階的に管理 */
    private String[] s = {"normal", "fast", "so fast"};
    private Color[] sc = {new Color(0, 170, 0), new Color(250, 120, 0), new Color(255, 0, 0)};

    public Speed(){
        this.setBounds(20, 0, 200, 50);
        this.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        updatesp(0);
    }

    /* スピードを更新 */
    public void updatesp(int n){
        setForeground(sc[n]);
        setText("speed : " + s[n]);
    }
}


////////////////////////////////////////////////////////
/* 自転車のクラス */

class Bicycle{
    /* x座標（固定）*/
    public static final int x = Macro.max / 3;

    /* 大きさ（固定）*/
    public static final int r = Macro.bs;

    /* y座標の初期値（固定） */
    public static final int dy = Macro.gh - Macro.bs;

    /* y座標（可変） */
    private int y = dy;

    /* 動き（通常、ジャンプ、落下） の判定
     * 0 : 通常
     * 1 : ジャンプ
     * 2 : 落下 */
    private int mj = 0;

    /* 動いている時間 */
    private int mt = 0;

    /* 画像の取り込み */
    private Image chari = Toolkit.getDefaultToolkit().getImage("image/chari20.png");


    /* 描画 */
    /* 画像の回転 : 座標軸回転 -> 描画 -> 座標軸戻す */
    public void draw(Graphics g, JPanel p){
        if(mj == 0){
            g.drawImage(chari, x, y, p);
        }else{

            Graphics2D g2 = (Graphics2D)g;
            AffineTransform at = g2.getTransform();

            /* arctan関数を用いて角度を計算 */
            /* 座標軸回転 */
            if(mj == 1){
                at.rotate(-Math.atan(0.4*Math.cos(-0.94+0.04*mt)), x+10, y+10);
            }else{
                at.rotate(Math.atan(0.08*mt), x+10, y+10);
            }
            g2.setTransform(at);

            /* 描画 */
            g2.drawImage(chari, x, y, p);

            /* 座標軸戻す */
            at.setToIdentity();
            g2.setTransform(at);
        }
    }

    public void setMJ(int j){
        mj = j;
    }

    public void setY(int y){
        this.y = y;
    }

    /* y座標の更新 */
    public void ymove(){
        if(mj == 0){
            return;

        /* jump : sin関数を利用 */
        }else if(mj == 1){
            y = (int)(dy - Macro.jh * Math.sin(0.025*mt));
            mt++;
            if(y > Macro.gh-Macro.bs){
                y = Macro.gh - Macro.bs;
                mj = 0;
                mt = 0;
            }

        /* fall : 放物線を描く */
        }else{
            y = (int)(0.04 * mt*mt + dy);
            mt++;
        }
    }

    public int getMJ(){
        return mj;
    }

    public int getY(){
        return y;
    }
}


/////////////////////////////////////////////////////
// model

class ChariModel extends Observable{

    /***** data *****/

    protected int i, j = 0;

    /* 地面 */
    protected Ground gr;

    /* 自転車 */
    protected Bicycle bi;

    /* 走行距離表示 */
    protected Mileage mi;

    /* スピード表示 */
    protected Speed sp;

    /* 背景 */
    protected Background bg;

    /* 自転車がいるところの地面の高さ知るための値 */
    /* bike left / right x */
    protected int blx = Bicycle.x + 100;
    protected int brx = Bicycle.x + Bicycle.r + 100;

    /* 新しい地面、穴or障害物設定していいかの判定 */
    protected boolean gj;

    /* 地面に関するカウント */
    protected int gc;

    /* 乱数 */
    protected double ran;

    /* ランダムな幅 */
    protected int rw;


    /***** constructor *****/

    public ChariModel(){
        /* 地面 */
        gr = new Ground();

        /* 自転車 */
        bi = new Bicycle();

        /* 走行距離 */
        mi = new Mileage();

        /* スピード */
        sp = new Speed();

        /* 背景 */
        bg = new Background();

        /* 地面生成に関する判定 */
        gj = true;

        /* 地面生成に関するカウント */
        gc = 0;

    }


    /***** method *****/

    /* 背景 */
    public Background getBG(){
        return bg;
    }

    /* 地面 */
    public Ground getGround(){
        return gr;
    }

    /* 自転車 */
    public Bicycle getBike(){
        return bi;
    }
    /* 自転車のジャンプ */
    public void jump(){
        bi.setMJ(1);
    }
    /* 自転車の落下 */
    public void fall(){
        bi.setMJ(2);
    }

    /* 走行距離 */
    public Mileage getMile(){
        return mi;
    }

    /* スピード */
    public Speed getSpeed(){
        return sp;
    }


    /* 主要部分の動き */
    public void pass(){

        /* 地面 */
        gr.move(j);

        /* 穴や障害物の作成 */
        if(gc == 0){
            ran = Math.random();
            rw = (int)(25+25*ran);

            /* 穴 */
            if(ran < 0.3 || ran > 0.8) gr.create_hole(j, rw);
            /* 障害物 */
            else gr.create_obst(j, rw, (int)(30+30*(1-ran)));

            gj = false;
            gc++;

        }else{
            gc++;
            if(gc >= rw) gj = true;
            if(gc > 150+300*ran) gc = 0;
        }

        j++;  blx++;  brx++;
        /* 座標がある値を越えたら新しい座標を振りなおす */
        if(j > Macro.gw) j = 0;
        if(blx > Macro.gw) blx = 0;
        if(brx > Macro.gw) brx = 0;

        /* 地面の初期化 */
        if(gj) gr.init(j);


        /* 自転車 */
        bi.ymove();

        /* 走行距離 */
        mi.updatem();


        /* Viewに通知 */
        setChanged();
        notifyObservers();

    }


    /* 当たり判定 */
    public void judge(){

        /* 衝突判定 */
        if(gr.getY(brx) < Macro.max
                && bi.getY() + Macro.bs > gr.getY(blx)
                || bi.getY() + Macro.bs > gr.getY(brx)){

            /* Viewに通知 */
            setChanged();
            notifyObservers("cd");

        }

        /* 穴への落下の判定 */
        if(bi.getY() == Macro.gh-Bicycle.r
                && gr.getY(brx) == Macro.max){
            fall();
        }

    }


    /* 一時停止の判定 */
    public void pause_judge(){
        setChanged();
        notifyObservers("pj");
    }


    /* 背景の動き */
    public void backpass(){
        bg.xmove();

        setChanged();
        notifyObservers();
    }

    /* スピード表示の更新 */
    public void change_speed(int n){
        sp.updatesp(n);
    }

}


///////////////////////////////////////////////////////////////
/* controller */

class ChariController implements KeyListener, MouseListener{

    /* data */

    /* 主要部分用のタイマー */
    protected javax.swing.Timer timer1;

    /* 背景用のタイマー*/
    protected javax.swing.Timer timer2;

    /* Model */
    protected ChariModel model;

    /* キー操作に関する判定 */
    /*（長押ししている間ジャンプし続けることを回避）*/
    protected boolean kj = true;

    /* タイマーの間隔 */
    protected int td1, td2;

    /* スピードのカウント */
    protected int sc = 0;


    /* constructor */

    public ChariController(ChariModel m){
        model = m;

        /* 全体用 */
        timer1 = new javax.swing.Timer(Macro.td, MainAction);

        /* 背景用 */
        timer2 = new javax.swing.Timer(Macro.td*6, BackAction);
    }


    /* タイマー */

    public void start_timer(){
        timer1.start();
        timer2.start();
    }

    public void restart_timer(){
        timer1.restart();
        timer2.restart();
    }

    public void stop_timer(){
        timer1.stop();
        timer2.stop();
    }


    /* インタフェースのメソッドの定義 */

    /* 全体用 */
    private ActionListener MainAction = new ActionListener(){
        public void actionPerformed(ActionEvent e){
            model.pass();
            model.judge();
        }
    };

    /* 背景用 */
    private ActionListener BackAction = new ActionListener(){
        public void actionPerformed(ActionEvent e){
            model.backpass();
        }
    };


    /* キー操作 */
    public void keyPressed(KeyEvent e){
        int k = e.getKeyCode();
        switch(k){
            /* ジャンプ */
            case KeyEvent.VK_UP:
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                if(model.getBike().getY() == Macro.gh-Bicycle.r && kj){
                    model.jump();
                    kj = false;     // release されるまでは 0
                }
                break;

            /* スピードアップ */
            case KeyEvent.VK_RIGHT:
                if(timer1.getDelay() > Macro.td-2){
                    timer1.setDelay(timer1.getDelay()-1);
                    timer2.setDelay(timer2.getDelay()/2);
                    sc++;
                    model.change_speed(sc);
                }
                break;

            /* スピードダウン */
            case KeyEvent.VK_LEFT:
                if(timer1.getDelay() < Macro.td){
                    timer1.setDelay(timer1.getDelay()+1);
                    timer2.setDelay(timer2.getDelay()*2);
                    sc--;
                    model.change_speed(sc);
                }
                break;
        }
    }

    public void keyTyped(KeyEvent e){
        int c = e.getKeyChar();
        switch(c){
            /* ジャンプ */
            case 'j':
            case 'w':
                if(model.getBike().getY() == Macro.gh-Bicycle.r && kj){
                    model.jump();
                    kj = false;
                }
                break;

            /* スピードアップ */
            case 'd':
                if(timer1.getDelay() > Macro.td-2){
                    timer1.setDelay(timer1.getDelay()-1);
                    timer2.setDelay(timer2.getDelay()/2);
                    sc++;
                    model.change_speed(sc);
                }
                break;

            /* スピードダウン */
            case 'a':
                if(timer1.getDelay() < Macro.td){
                    timer1.setDelay(timer1.getDelay()+1);
                    timer2.setDelay(timer2.getDelay()*2);
                    sc--;
                    model.change_speed(sc);
                }
                break;

            /* 一時停止 */
            case 'p':
                model.pause_judge();
                break;
        }
    }

    public void keyReleased(KeyEvent e){
        kj = true;
    }

    /* クリック操作（ジャンプ）*/
    public void mouseClicked(MouseEvent e){
        if(model.getBike().getY() == Macro.gh-Bicycle.r && kj){
            model.jump();
            kj = false;
        }
    }

    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mousePressed(MouseEvent e){}
    public void mouseReleased(MouseEvent e){}

}


//////////////////////////////////////////////////////////////////////////////
/* main view */

class ChariView extends JPanel implements Observer{

    /* data */

    /* Model */
    protected ChariModel model;

    /* Controller */
    protected ChariController cont;

    /* Frame */
    protected Chariso frame;

    protected int n = 0;


    /* constructor */

    public ChariView(ChariModel m, ChariController c, Chariso f){
        this.setLayout(null);   /* Layoutをnullにすることで配置の管理しやすくする */
        this.setBackground(Color.white);
        this.setPreferredSize(new Dimension(Macro.max, Macro.max));
        this.setFocusable(true);
        this.addKeyListener(c);
        this.addMouseListener(c);
        frame = f;
        model = m;
        cont = c;
        model.addObserver(this);
    }


    /* method */

    /* 描画 */
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        model.getBG().draw(g, this);
        model.getGround().draw(g);
        model.getBike().draw(g, this);
        this.add(model.getMile());
        this.add(model.getSpeed());
    }

    /* Modelからの通知を受け取って適切な処理を行う */
    public void update(Observable o, Object arg){
        repaint();

        /* 衝突 */
        if(arg == "cd"){
            cont.stop_timer();
            frame.ChangePanel(2);

        /* 一時停止 */
        }else if(arg == "pj"){
            cont.stop_timer();
            this.add(new PausePanel(frame, cont));
        }
    }

}


//////////////////////////////////////////////////////////////////////
/* スタート画面 */

class StartPanel extends JPanel implements ActionListener{

    private Chariso frame;
    private ChariController cont;
    private JLabel l;
    private JButton b;


    public StartPanel(ChariController c, Chariso f){

        frame = f;
        cont = c;

        this.setBackground(new Color(190,255,255));
        this.setPreferredSize(new Dimension(Macro.max, Macro.max));
        this.setLayout(null);

        l = new JLabel("<html><img src='file:image/title.png' width=300 height=300></html>", JLabel.CENTER);
        l.setBounds(80,50,300,300);

        /* ゲームをスタートさせるためのボタン */
        b = new JButton("Start");
        b.setFont(new Font(Font.MONOSPACED, Font.BOLD, 45));
        b.setBounds(100,325,300,100);
        b.addActionListener(this);

        this.add(l);
        this.add(b);

    }


    public void actionPerformed(ActionEvent e){
        /* ゲーム開始のための処理 */
        frame.ChangePanel(1);
        cont.start_timer();
    }

}


////////////////////////////////////////////////////////////////////////
/* ゲームオーバー画面 */

class GameoverPanel extends JPanel implements ActionListener{

    private Chariso frame;
    private ChariModel model;
    private JLabel l1;
    private JLabel l2;
    private JButton b;
    private String str;

    public GameoverPanel(Chariso f, ChariModel m){

        frame = f;
        model = m;

        this.setBackground(new Color(255,50,50));
        this.setPreferredSize(new Dimension(Macro.max, Macro.max));
        this.setLayout(null);

        l1 = new JLabel("<html><img src='file:image/gameover.png' width=220 height=150></html>", JLabel.CENTER);
        l1.setBounds(155,50,220,150);

        /* スコア表示 */
        str = model.getMile().getMile();
        l2 = new JLabel("Score: " + str + " m", JLabel.CENTER);
        l2.setFont(new Font(Font.MONOSPACED,Font.BOLD,30));
        l2.setBounds(10,255,480,50);

        /* ゲームを再プレイするためのボタン */
        b = new JButton("Play Again");
        b.setFont(new Font(Font.MONOSPACED, Font.BOLD, 40));
        b.setBounds(100,350,300,100);
        b.addActionListener(this);

        this.add(l1);
        this.add(l2);
        this.add(b);
    }


    public void actionPerformed(ActionEvent e){
        /* ゲーム初期化の処理 */
        frame.RemovePanel();
        frame.InitPanel();
    }

}


////////////////////////////////////////////////////////////////////////
/* 一時停止のポップアップ */

class PausePanel extends JPanel implements ActionListener{

    private Chariso frame;
    private ChariController cont;
    private JLabel l1;
    private JButton b1, b2;


    public PausePanel(Chariso f, ChariController c){

        this.setBackground(new Color(190, 255, 255, 150));  /* 半透明にしてゲーム画面に被せる */
        this.setBounds(80, 80, 340, 340);
        this.setLayout(null);

        frame = f;
        cont = c;

        l1 = new JLabel("Paused", JLabel.CENTER);
        l1.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 55));
        l1.setBounds(50, 30, 240, 70);

        /* ゲーム再開のためのボタン */
        b1 = new JButton("Continue");
        b1.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 35));
        b1.setBounds(30, 140, 280, 55);
        b1.setBackground(new Color(255, 255, 255));

        /* タイトル画面に戻るためのボタン */
        b2 = new JButton("Back to Title");
        b2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        b2.setBounds(30, 230, 280, 55);
        b2.setBackground(new Color(255, 255, 255));

        b1.addActionListener(this);
        b2.addActionListener(this);
        this.add(l1);
        this.add(b1);
        this.add(b2);

    }


    public void actionPerformed(ActionEvent e){
        /* ゲーム再開 */
        if(e.getSource() == b1){
            cont.restart_timer();
            this.setVisible(false);

        /* タイトルに戻る */
        }else{
            frame.ChangePanel(2);
            frame.RemovePanel();
            frame.InitPanel();
        }
    }

}


//////////////////////////////////////////////////////////////////////////////
/* Main */

class Chariso extends JFrame{

    /* Model */
    private ChariModel model;

    /* Controller */
    private ChariController cont;

    /* View  */
    private ChariView view;

    /* スタート画面 */
    private StartPanel sp;

    /* ゲームオーバー画面 */
    private GameoverPanel gp;

    /* 画面遷移に利用する配列 */
    private JPanel[] p = {sp, view, gp};

    /* 一時停止のポップアップ */
    private PausePanel pp;


    public Chariso(){
        this.setTitle("Chariso");
        this.setLocation(50, 50);
        InitPanel();
        this.pack();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);   /* ウィンドウの形は変更不能 */
        this.setVisible(true);
    }


    /* panel 操作 */

    /* 初期化 */
    public void InitPanel(){
        model = new ChariModel();
        cont = new ChariController(model);
        p[0] = new StartPanel(cont, this);
        p[1] = new ChariView(model, cont, this);
        this.add(p[0]);
    }

    /* 画面遷移 */
    public void ChangePanel(int n){
        if(n == 2) p[2] = new GameoverPanel(this, model);
        p[n-1].setVisible(false);
        this.add(p[n]);
        p[n].requestFocus();
    }

    /* 全パネルを取り外す */
    public void RemovePanel(){
        p[2].setVisible(false);
        for(int i = 0; i < 3; i++) this.remove(p[i]);
    }


    /* main */
    public static void main(String argv[]){
        new Chariso();
    }

}

