package com.lan.screencut;

import com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

/**
 * 截图工具类
 *
 * @author ajtdnyy
 */
public class ScreenCut extends javax.swing.JDialog implements MouseListener, MouseMotionListener, ActionListener {

    /**
     * 是否确定开始点
     */
    private boolean isStart;
    /**
     * 是否确定结束点
     */
    private boolean isEnd;
    /**
     * 是否可以开始绘制
     */
    private boolean isDraw;
    /**
     * 是否开始拖曳
     */
    private boolean isDrag;
    /**
     * 是否整个框移动
     */
    private boolean isMove;
    /**
     * 是否显示工具栏
     */
    private boolean isShow;
    /**
     * 是否正在绘制
     */
    private boolean isDrawing;
    /**
     * 鼠标所有边框位置
     */
    private int dir;
    /**
     * 拖曳终止点
     */
    private Point end = new Point();
    /**
     * 拖曳开始点
     */
    private Point begin = new Point();
    /**
     * 绘图起始点
     */
    private Point drawStart = new Point();
    /**
     * 绘图终止点
     */
    private Point drawEnd = new Point();
    /**
     * 用于检测的矩形框
     */
    private Rectangle smallRect = new Rectangle();
    /**
     * 调整边框起始点与边框起始点相对距离
     */
    private int dx, dy, dx2, dy2;
    private List<Object[]> draws = new ArrayList<Object[]>();
    private Cursor cur;
    private static File file;

    /**
     * Creates new form ScreenCut
     *
     * @param parent
     * @param modal
     */
    public ScreenCut(java.awt.Frame parent, boolean modal) {
        initComponents();
        addMouseListener(this);
        addMouseMotionListener(this);
        toolbar.setVisible(false);
        panel.setVisible(false);
        text.setVisible(false);
        try {
            robot = new Robot();
            screen = new Rectangle(tool.getScreenSize());
            setSize(screen.width, screen.height);
            bgimg = robot.createScreenCapture(screen);
            cur = tool.createCustomCursor(tool.createImage(getClass().getResource("/cur.png")), new Point(0, 0), "cur");
            setCursor(cur);
            ButtonGroup bg = new ButtonGroup();
            bg.add(size1);
            bg.add(size2);
            bg.add(size3);
            initActionListener(panel.getComponents());
            initActionListener(toolbar.getComponents());
        } catch (AWTException ex) {
            Logger.getLogger(ScreenCut.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initActionListener(Component[] cps) {
        for (Component cp : cps) {
            if (cp instanceof AbstractButton) {
                AbstractButton ccp = (AbstractButton) cp;
                ccp.addActionListener(this);
            }
        }
    }

    /**
     * 使工具按钮处于单选
     *
     * @param bt
     */
    private void initButton(JToggleButton bt) {
        rectbtn.setSelected(bt.equals(rectbtn));
        circlebtn.setSelected(bt.equals(circlebtn));
        dirbtn.setSelected(bt.equals(dirbtn));
        fontbtn.setSelected(bt.equals(fontbtn));
        if (!fontbtn.isSelected()) {
            addText();
        }
    }

    private boolean isSelection() {
        return rectbtn.isSelected() || circlebtn.isSelected() || dirbtn.isSelected() || fontbtn.isSelected();
    }

    /**
     * 绘制阴影
     *
     * @param g
     */
    private void drawLines(Graphics g) {
        g.setColor(Color.gray);
        for (int i = 0; i < screen.height; i += 8) {
            g.drawLine(0, i, Math.min(begin.x, end.x) - 2, i);
            g.drawLine(Math.max(begin.x, end.x) + 2, i, screen.width, i);
            if (i < Math.min(begin.y, end.y) || i > Math.max(begin.y, end.y)) {
                g.drawLine(Math.min(begin.x, end.x) - 2, i, Math.max(begin.x, end.x) + 2, i);
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(bgimg, 0, 0, null);
        drawLines(g);
        if (isDraw) {
            drawRect(begin, end, Color.red, g, 1);
            //四个顶点
            g.fillRect(begin.x - 2, begin.y - 2, range, range);
            g.fillRect(end.x - 2, begin.y - 2, range, range);
            g.fillRect(begin.x - 2, end.y - 2, range, range);
            g.fillRect(end.x - 2, end.y - 2, range, range);
            //四个中点
            g.fillRect(smallRect.x + smallRect.width / 2 - 2, begin.y - 2, range, range);
            g.fillRect(smallRect.x + smallRect.width / 2 - 2, end.y - 2, range, range);
            g.fillRect(begin.x - 2, smallRect.y + smallRect.height / 2 - 2, range, range);
            g.fillRect(end.x - 2, smallRect.y + smallRect.height / 2 - 2, range, range);
            //边框大小
            g.setColor(Color.BLACK);
            int x = smallRect.x - 5, y = smallRect.y - 28;
            y = y < -5 ? 2 : y;
            g.fillRect(x, y, 80, 20);
            g.setColor(Color.white);
            g.drawString(Math.abs(begin.x - end.x) + "×" + Math.abs(begin.y - end.y), x + 5, y + 18);
            //工具栏
            int totalHeight = toolbar.getHeight() + panel.getHeight();
            x = smallRect.x + smallRect.width + range - toolbar.getWidth();
            x = x < 0 ? 2 : x;
            y = smallRect.y + smallRect.height + 3 * range;
            if (y > screen.height - totalHeight) {
                y = smallRect.y - toolbar.getHeight() - 3 * range;
                toolbar.setLocation(x, y);
                panel.setLocation(toolbar.getX(), y - totalHeight + 3 * range);
            } else {
                toolbar.setLocation(x, y);
                panel.setLocation(toolbar.getX(), toolbar.getY() + toolbar.getHeight() + 2);
            }
            toolbar.setVisible(isShow);
            panel.setVisible(isShow && isSelection());
        }
        if (isDrawing) {
            if (rectbtn.isSelected()) {
                drawRect(drawStart, drawEnd, displaySelected.getBackground(), g, thick);
            } else if (circlebtn.isSelected()) {
                drawRoundRect(drawStart, drawEnd, displaySelected.getBackground(), g);
            } else if (dirbtn.isSelected()) {
                drawDir(drawStart, drawEnd, displaySelected.getBackground(), g);
            }
        }
        //绘制图形
        for (Iterator<Object[]> it = draws.iterator(); it.hasNext();) {
            Object[] pts = it.next();
            if (pts[3].toString().equals("rect")) {
                drawRect((Point) pts[0], (Point) pts[1], (Color) pts[2], g, Integer.valueOf(pts[4].toString()));
            } else if (pts[3].toString().equals("circle")) {
                drawRoundRect((Point) pts[0], (Point) pts[1], (Color) pts[2], g);
            } else if (pts[3].toString().equals("dir")) {
                drawDir((Point) pts[0], (Point) pts[1], (Color) pts[2], g);
            } else {
                drawString((Point) pts[0], pts[3].toString(), (Color) pts[2], g, Integer.valueOf(pts[4].toString()));
            }
        }
        toolbar.repaint();
        panel.repaint();
    }

    /**
     * 绘制文字
     */
    private void drawString(Point st, String str, Color c, Graphics g, int size) {
        g.setFont(new Font("宋体", Font.PLAIN, size));
        g.setColor(c);
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D r2d = fm.getStringBounds(str, g);
        g.drawString(str, st.x, st.y + (int) r2d.getHeight());
    }

    /**
     * 绘制方形
     */
    private void drawRect(Point st, Point ed, Color c, Graphics g, int thick) {
        g.setColor(c);
        g.fillRect(Math.min(st.x, ed.x), st.y, Math.abs(ed.x - st.x), thick);//横线1
        g.fillRect(Math.min(st.x, ed.x), ed.y, Math.abs(ed.x - st.x) + thick, thick);//横线2
        g.fillRect(st.x, Math.min(st.y, ed.y), thick, Math.abs(ed.y - st.y));//竖线1
        g.fillRect(ed.x, Math.min(st.y, ed.y), thick, Math.abs(ed.y - st.y));//坚线2
    }

    /**
     * 绘制线条
     */
    private void drawDir(Point st, Point ed, Color c, Graphics g) {
        g.setColor(c);
        paintk(g, st.x, st.y, ed.x, ed.y);
    }

    /**
     * 绘制圆形
     */
    private void drawRoundRect(Point st, Point ed, Color c, Graphics g) {
        g.setColor(c);
        int w = Math.abs(st.x - ed.x);
        int h = Math.abs(st.y - ed.y);
        g.drawRoundRect(Math.min(st.x, ed.x), Math.min(st.y, ed.y), w, h, w, h);
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 2) {//鼠标右键双击退出截图
            dispose();
        }
    }

    public void mousePressed(MouseEvent e) {
        if (!isStart) {//初始化边框起始点
            isStart = true;
            begin.x = e.getX();
            begin.y = e.getY();
        } else if (isSelection() && isInRect(e.getX(), e.getY())) {//在截图边框内绘制其他图形起始点初始化
            drawStart.x = e.getX();
            drawStart.y = e.getY();
            if (fontbtn.isSelected()) {
                text.setLocation(drawStart);
                text.setVisible(true);
            }
        } else {
            if (isInRect(e.getX(), e.getY())) {
                isMove = true;
            } else {
                checkDIR(e.getX(), e.getY());
                if (dir > 0) {
                    isDrag = true;
                }
            }//初始化调整边框起始点相对位置
            dx = e.getX() - begin.x;
            dy = e.getY() - begin.y;
            dx2 = e.getX() - end.x;
            dy2 = e.getY() - end.y;
        }
        isShow = false;
        repaint();
    }

    public void mouseReleased(MouseEvent e) {
        if (isStart && !isEnd) {//首次绘制边框结束点
            end.x = e.getX();
            end.y = e.getY();
            isEnd = true;
        } else if (isDrawing && isInRect(e.getX(), e.getY())) {
            drawEnd.x = e.getX();
            drawEnd.y = e.getY();
            String type = rectbtn.isSelected() ? "rect" : circlebtn.isSelected() ? "circle" : dirbtn.isSelected() ? "dir" : "";
            if (!type.isEmpty()) {
                draws.add(new Object[]{
                    drawStart.clone(), drawEnd.clone(), displaySelected.getBackground(), type, thick
                });
            }
            isDrawing = false;
        }
        int x = end.x, y = end.y;//交换起始点结束点坐标，使起始点保持在左上角位置
        end.x = Math.max(end.x, begin.x);
        end.y = Math.max(end.y, begin.y);
        begin.x = Math.min(x, begin.x);
        begin.y = Math.min(y, begin.y);
        isDrag = false;
        isMove = false;
        isShow = true;
        repaint();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        if (!isEnd) {//首次拖曳未完成时动态绘制边框
            end.x = e.getX();
            end.y = e.getY();
            isDraw = true;
        } else if (isSelection() && isInRect(e.getX(), e.getY())) {//绘制图形
            drawEnd.x = e.getX();
            drawEnd.y = e.getY();
            isDrawing = true;
        } else {//首次绘制边框完成后
            if (isMove && draws.isEmpty()) {//整个边框移动
                int sx = e.getX() - dx, sy = e.getY() - dy, ex = e.getX() - dx2, ey = e.getY() - dy2;
                int sxo = begin.x, syo = begin.y, exo = end.x, eyo = end.y;
                begin.x = sx;
                begin.y = sy;
                end.x = ex;
                end.y = ey;
                if (Math.min(sx, ex) < 0 || Math.min(sy, ey) < 0 || Math.max(sx, ex) > screen.width || Math.max(sy, ey) > screen.height) {
                    begin.x = sxo;
                    begin.y = syo;
                    end.x = exo;
                    end.y = eyo;
                }
            } else if (isDrag) {//大小调整
                switch (dir) {
                    case LEFT:
                        begin.x = e.getX();
                        break;
                    case RIGHT:
                        end.x = e.getX();
                        break;
                    case LEFT_UP:
                        begin.y = e.getY();
                        begin.x = e.getX();
                        break;
                    case RIGHT_DOWN:
                        end.y = e.getY();
                        end.x = e.getX();
                        break;
                    case RIGHT_UP:
                        begin.y = e.getY();
                        end.x = e.getX();
                        break;
                    case LEFT_DOWN:
                        begin.x = e.getX();
                        end.y = e.getY();
                        break;
                    case DOWN:
                        end.y = e.getY();
                        break;
                    case UP:
                        begin.y = e.getY();
                        break;
                }
            }
        }
        isInRect(dx, dy);
        isShow = false;
        repaint();
    }

    public void mouseMoved(MouseEvent e) {
        if (isEnd) {
            int x = e.getX(), y = e.getY();
            if (isInRect(x, y)) {
                if (isSelection()) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                } else if (draws.isEmpty()) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else {
                    setCursor(cur);
                }
            } else {
                checkDIR(x, y);
                switch (dir) {
                    case LEFT:
                    case RIGHT:
                        setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                        break;
                    case LEFT_UP:
                    case RIGHT_DOWN:
                        setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                        break;
                    case RIGHT_UP:
                    case LEFT_DOWN:
                        setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                        break;
                    case DOWN:
                    case UP:
                        setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                        break;
                    default:
                        setCursor(cur);
                }
            }
        }
    }

    /**
     * 判断鼠标是否在矩形内
     *
     * @param x
     * @param y
     * @return
     */
    private boolean isInRect(int x, int y) {
        smallRect = new Rectangle();
        smallRect.x = Math.min(begin.x, end.x) + range;
        smallRect.y = Math.min(begin.y, end.y) + range;
        smallRect.height = Math.abs(begin.y - end.y) - 2 * range;
        smallRect.width = Math.abs(begin.x - end.x) - 2 * range;
        return smallRect.contains(x, y);
    }

    /**
     * 判断鼠标方向
     *
     * @param x
     * @param y
     */
    private void checkDIR(int x, int y) {
        Point st = new Point();
        st.x = Math.min(begin.x, end.x);
        st.y = Math.min(begin.y, end.y);
        if (x - range < st.x && x + range > st.x) {
            if (y > st.y + range && y < st.y + smallRect.height + range) {
                dir = LEFT;
            } else if (y >= st.y - range && y <= st.y + range) {
                dir = LEFT_UP;
            } else if (y >= st.y + smallRect.height + range && y <= st.y + smallRect.height + 3 * range) {
                dir = LEFT_DOWN;
            } else {
                dir = -1;
            }
        } else if (x - range < st.x + smallRect.width + 2 * range && x + range > st.x + smallRect.width + 2 * range) {
            if (y > st.y + range && y < st.y + smallRect.height + range) {
                dir = RIGHT;
            } else if (y >= st.y - range && y <= st.y + range) {
                dir = RIGHT_UP;
            } else if (y >= st.y + smallRect.height + range && y <= st.y + smallRect.height + 3 * range) {
                dir = RIGHT_DOWN;
            } else {
                dir = -1;
            }
        } else if (y - range < st.y + smallRect.height + 2 * range && y + range > st.y + smallRect.height + 2 * range) {
            if (x > st.x + range && x < st.x + smallRect.width + range) {
                dir = DOWN;
            } else {
                dir = -1;
            }
        } else if (y - range < st.y && y + range > st.y) {
            if (x > st.x + range && x < st.x + smallRect.width + range) {
                dir = UP;
            } else {
                dir = -1;
            }
        } else {
            dir = -1;
        }
        dir = draws.isEmpty() ? dir : -1;
    }

    private Rectangle getNormalRect() {
        Rectangle nor = (Rectangle) smallRect.clone();
        nor.x -= range;
        nor.y -= range;
        nor.height += 2 * range;
        nor.width += 2 * range;
        return nor;
    }

    /**
     * 画带箭头的线
     */
    public void paintk(Graphics g, int x1, int y1, int x2, int y2) {
        double H = 10;  // 箭头高度
        double L = 7; // 底边的一半
        int x3 = 0;
        int y3 = 0;
        int x4 = 0;
        int y4 = 0;
        double awrad = Math.atan(L / H);  // 箭头角度
        double arraow_len = Math.sqrt(L * L + H * H); // 箭头的长度
        double[] arrXY_1 = rotateVec(x2 - x1, y2 - y1, awrad, true, arraow_len);
        double[] arrXY_2 = rotateVec(x2 - x1, y2 - y1, -awrad, true, arraow_len);
        double x_3 = x2 - arrXY_1[0];  // (x3,y3)是第一端点
        double y_3 = y2 - arrXY_1[1];
        double x_4 = x2 - arrXY_2[0]; // (x4,y4)是第二端点
        double y_4 = y2 - arrXY_2[1];

        Double X3 = new Double(x_3);
        x3 = X3.intValue();
        Double Y3 = new Double(y_3);
        y3 = Y3.intValue();
        Double X4 = new Double(x_4);
        x4 = X4.intValue();
        Double Y4 = new Double(y_4);
        y4 = Y4.intValue();
        // 画线
        g.drawLine(x1, y1, x2, y2);
        // 画箭头的一半
        g.drawLine(x2, y2, x3, y3);
        // 画箭头的另一半
        g.drawLine(x2, y2, x4, y4);
    }

    /**
     * 取得箭头的绘画范围
     *
     * @param px
     * @param py
     * @param ang
     * @param isChLen
     * @param newLen
     * @return
     */
    public double[] rotateVec(int px, int py, double ang, boolean isChLen, double newLen) {
        double mathstr[] = new double[2];
        // 矢量旋转函数，参数含义分别是x分量、y分量、旋转角、是否改变长度、新长度
        double vx = px * Math.cos(ang) - py * Math.sin(ang);
        double vy = px * Math.sin(ang) + py * Math.cos(ang);
        if (isChLen) {
            double d = Math.sqrt(vx * vx + vy * vy);
            vx = vx / d * newLen;
            vy = vy / d * newLen;
            mathstr[0] = vx;
            mathstr[1] = vy;
        }
        return mathstr;
    }

    private void initFileChooser(JFileChooser jfc) {
        jfc.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(".png");
            }

            @Override
            public String getDescription() {
                return "*.png";
            }
        });
        jfc.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(".bmp");
            }

            @Override
            public String getDescription() {
                return "*.bmp";
            }
        });
        jfc.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(".gif");
            }

            @Override
            public String getDescription() {
                return "*.gif";
            }
        });
        jfc.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(".jpeg") || name.endsWith(".jpg");
            }

            @Override
            public String getDescription() {
                return "*.jpg";
            }
        });
    }

    private void saveFile(final File file) {
        isDraw = false;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    image = robot.createScreenCapture(getNormalRect());
                    String name = file.getName();
                    ImageIO.write(image, name.substring(name.indexOf(".") + 1), file);
                } catch (IOException ex) {
                    Logger.getLogger(ScreenCut.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    dispose();
                }
            }
        }, 50);
    }

    private void addText() {
        text.setVisible(false);
        if (!text.getText().isEmpty()) {
            draws.add(new Object[]{
                drawStart.clone(), drawEnd.clone(), displaySelected.getBackground(), text.getText(), fontSize.getSelectedItem()
            });
            text.setText("");
            repaint();
        }
    }

    @Override
    public void dispose() {
        JDialog.setDefaultLookAndFeelDecorated(true);
        super.dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        panel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        fontSize = new javax.swing.JComboBox();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jButton15 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jButton19 = new javax.swing.JButton();
        jButton20 = new javax.swing.JButton();
        jButton21 = new javax.swing.JButton();
        jButton22 = new javax.swing.JButton();
        displaySelected = new javax.swing.JButton();
        size1 = new javax.swing.JToggleButton();
        size3 = new javax.swing.JToggleButton();
        size2 = new javax.swing.JToggleButton();
        toolbar = new javax.swing.JToolBar();
        rectbtn = new javax.swing.JToggleButton();
        circlebtn = new javax.swing.JToggleButton();
        fontbtn = new javax.swing.JToggleButton();
        dirbtn = new javax.swing.JToggleButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        text = new javax.swing.JTextField();

        setAlwaysOnTop(true);
        setResizable(false);
        setUndecorated(true);
        getContentPane().setLayout(null);

        panel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 204)));
        panel.setPreferredSize(new java.awt.Dimension(320, 100));
        java.awt.GridBagLayout panelLayout = new java.awt.GridBagLayout();
        panelLayout.columnWidths = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        panelLayout.rowHeights = new int[] {0};
        panel.setLayout(panelLayout);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/font.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        panel.add(jLabel1, gridBagConstraints);

        fontSize.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "8", "9", "10", "11", "12", "14", "16", "18", "20" }));
        fontSize.setSelectedIndex(4);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        panel.add(fontSize, gridBagConstraints);

        jButton9.setBackground(new java.awt.Color(102, 102, 102));
        jButton9.setForeground(new java.awt.Color(102, 102, 102));
        jButton9.setActionCommand("color");
        jButton9.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 1;
        panel.add(jButton9, gridBagConstraints);

        jButton10.setBackground(new java.awt.Color(255, 255, 255));
        jButton10.setForeground(new java.awt.Color(255, 255, 255));
        jButton10.setActionCommand("color");
        jButton10.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 1;
        panel.add(jButton10, gridBagConstraints);

        jButton11.setBackground(new java.awt.Color(0, 0, 0));
        jButton11.setActionCommand("color");
        jButton11.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        panel.add(jButton11, gridBagConstraints);

        jButton12.setBackground(new java.awt.Color(255, 51, 255));
        jButton12.setForeground(new java.awt.Color(255, 51, 255));
        jButton12.setActionCommand("color");
        jButton12.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        panel.add(jButton12, gridBagConstraints);

        jButton13.setBackground(new java.awt.Color(153, 0, 0));
        jButton13.setForeground(new java.awt.Color(153, 0, 0));
        jButton13.setActionCommand("color");
        jButton13.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 0;
        panel.add(jButton13, gridBagConstraints);

        jButton14.setBackground(new java.awt.Color(255, 0, 0));
        jButton14.setForeground(new java.awt.Color(255, 0, 0));
        jButton14.setActionCommand("color");
        jButton14.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 1;
        panel.add(jButton14, gridBagConstraints);

        jButton15.setBackground(new java.awt.Color(255, 255, 51));
        jButton15.setForeground(new java.awt.Color(255, 255, 51));
        jButton15.setActionCommand("color");
        jButton15.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 1;
        panel.add(jButton15, gridBagConstraints);

        jButton16.setBackground(new java.awt.Color(255, 102, 102));
        jButton16.setForeground(new java.awt.Color(255, 102, 102));
        jButton16.setActionCommand("color");
        jButton16.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 0;
        panel.add(jButton16, gridBagConstraints);

        jButton17.setBackground(new java.awt.Color(0, 153, 153));
        jButton17.setForeground(new java.awt.Color(0, 153, 153));
        jButton17.setActionCommand("color");
        jButton17.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 12;
        gridBagConstraints.gridy = 0;
        panel.add(jButton17, gridBagConstraints);

        jButton18.setBackground(new java.awt.Color(51, 255, 204));
        jButton18.setForeground(new java.awt.Color(51, 255, 204));
        jButton18.setActionCommand("color");
        jButton18.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 12;
        gridBagConstraints.gridy = 1;
        panel.add(jButton18, gridBagConstraints);

        jButton19.setBackground(new java.awt.Color(0, 153, 0));
        jButton19.setForeground(new java.awt.Color(0, 153, 0));
        jButton19.setActionCommand("color");
        jButton19.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 0;
        panel.add(jButton19, gridBagConstraints);

        jButton20.setBackground(new java.awt.Color(0, 0, 153));
        jButton20.setForeground(new java.awt.Color(0, 0, 153));
        jButton20.setActionCommand("color");
        jButton20.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 11;
        gridBagConstraints.gridy = 0;
        panel.add(jButton20, gridBagConstraints);

        jButton21.setBackground(new java.awt.Color(0, 0, 51));
        jButton21.setForeground(new java.awt.Color(0, 0, 51));
        jButton21.setActionCommand("color");
        jButton21.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 11;
        gridBagConstraints.gridy = 1;
        panel.add(jButton21, gridBagConstraints);

        jButton22.setBackground(new java.awt.Color(51, 255, 51));
        jButton22.setForeground(new java.awt.Color(51, 255, 51));
        jButton22.setActionCommand("color");
        jButton22.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 1;
        panel.add(jButton22, gridBagConstraints);

        displaySelected.setBackground(new java.awt.Color(255, 0, 0));
        displaySelected.setFocusable(false);
        displaySelected.setPreferredSize(new java.awt.Dimension(40, 40));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        panel.add(displaySelected, gridBagConstraints);

        size1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/size1_off.png"))); // NOI18N
        size1.setSelected(true);
        size1.setActionCommand("size");
        size1.setFocusable(false);
        size1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        size1.setPreferredSize(new java.awt.Dimension(25, 30));
        size1.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/size1_on.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        panel.add(size1, gridBagConstraints);

        size3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/size3_off.png"))); // NOI18N
        size3.setActionCommand("size");
        size3.setFocusable(false);
        size3.setPreferredSize(new java.awt.Dimension(25, 30));
        size3.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/size3_on.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        panel.add(size3, gridBagConstraints);

        size2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/size2_off.png"))); // NOI18N
        size2.setActionCommand("size");
        size2.setFocusable(false);
        size2.setPreferredSize(new java.awt.Dimension(25, 30));
        size2.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/size2_on.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        panel.add(size2, gridBagConstraints);

        getContentPane().add(panel);
        panel.setBounds(80, 170, 280, 50);

        toolbar.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 51, 255)));
        toolbar.setFloatable(false);
        toolbar.setDoubleBuffered(true);

        rectbtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rect.png"))); // NOI18N
        rectbtn.setToolTipText("添加矩形");
        rectbtn.setFocusable(false);
        rectbtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        rectbtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(rectbtn);

        circlebtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/circle.png"))); // NOI18N
        circlebtn.setToolTipText("添加圆形");
        circlebtn.setFocusable(false);
        circlebtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        circlebtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(circlebtn);

        fontbtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/font.png"))); // NOI18N
        fontbtn.setToolTipText("添加文字");
        fontbtn.setFocusable(false);
        fontbtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        fontbtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(fontbtn);

        dirbtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dir.png"))); // NOI18N
        dirbtn.setToolTipText("添加箭头");
        dirbtn.setFocusable(false);
        dirbtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        dirbtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(dirbtn);

        jSeparator1.setPreferredSize(new java.awt.Dimension(1, 0));
        toolbar.add(jSeparator1);

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/undo.png"))); // NOI18N
        jButton5.setToolTipText("撤销编辑");
        jButton5.setActionCommand("undo");
        jButton5.setFocusable(false);
        jButton5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton5.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(jButton5);

        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/save.png"))); // NOI18N
        jButton6.setToolTipText("保存");
        jButton6.setActionCommand("save");
        jButton6.setFocusable(false);
        jButton6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton6.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(jButton6);

        jSeparator2.setPreferredSize(new java.awt.Dimension(1, 0));
        toolbar.add(jSeparator2);

        jButton7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/exit.png"))); // NOI18N
        jButton7.setToolTipText("退出截图");
        jButton7.setActionCommand("exit");
        jButton7.setFocusable(false);
        jButton7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton7.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(jButton7);

        jButton8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ok.png"))); // NOI18N
        jButton8.setToolTipText("完成截图");
        jButton8.setActionCommand("ok");
        jButton8.setFocusable(false);
        jButton8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton8.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(jButton8);

        getContentPane().add(toolbar);
        toolbar.setBounds(81, 138, 280, 25);

        text.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        text.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textKeyPressed(evt);
            }
        });
        getContentPane().add(text);
        text.setBounds(80, 100, 230, 30);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void textKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            addText();
        }
    }//GEN-LAST:event_textKeyPressed

    public static void main(String[] args) {
        ShowScreenCut(true);
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ("color".equals(cmd)) {
            JButton jb = (JButton) e.getSource();
            displaySelected.setBackground(jb.getBackground());
        } else {
            if ("exit".equals(cmd)) {
                dispose();
            } else if ("ok".equals(cmd)) {
                isDraw = false;
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        image = robot.createScreenCapture(getNormalRect());
                        tool.getSystemClipboard().setContents(new ImageChange(image), null);
                        dispose();
                    }
                }, 50);
            } else if ("save".equals(cmd)) {
                JFileChooser jfc = new JFileChooser();
                jfc.setCurrentDirectory(file);
                jfc.requestFocus();
                initFileChooser(jfc);
                int opt = jfc.showSaveDialog(getRootPane());
                if (opt == JFileChooser.APPROVE_OPTION) {
                    file = jfc.getSelectedFile();
                    String name = file.getName();
                    if (!name.contains(".")) {
                        String path = file.getPath() + jfc.getFileFilter().getDescription().substring(1);
                        file = new File(path);
                    }
                    if (file.exists()) {
                        opt = JOptionPane.showConfirmDialog(rootPane, "文件" + file.getName() + "已经存在,是否覆盖？");
                        if (opt == JOptionPane.OK_OPTION) {
                            saveFile(file);
                        }
                    } else {
                        saveFile(file);
                    }
                }
                requestFocus();
            } else if ("undo".equals(cmd)) {
                if (!draws.isEmpty()) {
                    draws.remove(draws.size() - 1);
                }
            } else if ("size".equals(cmd)) {
                thick = size1.isSelected() ? 1 : size2.isSelected() ? 2 : 3;
            } else {
                boolean eq = temp == null ? false : temp.equals((JToggleButton) e.getSource());
                clickCount = eq ? ++clickCount : 0;
                temp = (JToggleButton) e.getSource();
                initButton(temp);
                temp.setSelected(clickCount % 2 == 0);
            }
            boolean flag = fontbtn.isSelected();
            jLabel1.setVisible(flag);
            fontSize.setVisible(flag);
            flag = rectbtn.isSelected();
            size1.setVisible(flag);
            size2.setVisible(flag);
            size3.setVisible(flag);
        }
        toolbar.repaint();
        panel.repaint();
        repaint();
    }

    /**
     * 调用此截图工具的应用程序未使用第三方皮肤时 nolnf=true 否则传false
     *
     * @param nolnf
     */
    public static void ShowScreenCut(boolean nolnf) {
        try {
            if (nolnf) {
                UIManager.setLookAndFeel(new NimbusLookAndFeel());
            }
        } catch (Exception ex) {
            Logger.getLogger(ScreenCut.class.getName()).log(Level.SEVERE, null, ex);
        }
        JDialog.setDefaultLookAndFeelDecorated(false);
        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ScreenCut dialog = new ScreenCut(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton circlebtn;
    private javax.swing.JToggleButton dirbtn;
    private javax.swing.JButton displaySelected;
    private javax.swing.JComboBox fontSize;
    private javax.swing.JToggleButton fontbtn;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton19;
    private javax.swing.JButton jButton20;
    private javax.swing.JButton jButton21;
    private javax.swing.JButton jButton22;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JPanel panel;
    private javax.swing.JToggleButton rectbtn;
    private javax.swing.JToggleButton size1;
    private javax.swing.JToggleButton size2;
    private javax.swing.JToggleButton size3;
    private javax.swing.JTextField text;
    private javax.swing.JToolBar toolbar;
    // End of variables declaration//GEN-END:variables
    private Robot robot;
    /**
     * 画方形的粗细
     */
    private int thick = 1;
    private int clickCount;
    private Rectangle screen;
    private JToggleButton temp;
    private BufferedImage bgimg;
    private BufferedImage image;
    private Toolkit tool = Toolkit.getDefaultToolkit();
    private final int range = 5;
    private final int UP = 4;
    private final int LEFT = 2;
    private final int DOWN = 3;
    private final int RIGHT = 1;
    private final int LEFT_UP = 5;
    private final int RIGHT_UP = 7;
    private final int LEFT_DOWN = 6;
    private final int RIGHT_DOWN = 8;

    class ImageChange implements Transferable {

        private BufferedImage theImage;

        public ImageChange(BufferedImage image) {
            theImage = image;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{
                DataFlavor.imageFlavor
            };
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(DataFlavor.imageFlavor);
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(DataFlavor.imageFlavor)) {
                return theImage;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }
}
