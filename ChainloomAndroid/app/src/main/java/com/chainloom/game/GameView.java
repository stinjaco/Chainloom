package com.chainloom.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/** Dependency-free Canvas UI: one Activity, one custom View, one pure-Java game engine. */
public final class GameView extends View {
    private static final int SCREEN_MENU = 0;
    private static final int SCREEN_GAME = 1;
    private static final int SCREEN_HELP = 2;
    private static final long DAILY_SEED_SALT = 0x434841494E4C4F4FL;
    private static final String PREFS = "chainloom_prefs";
    private static final String SAVE_KEY = "saved_game_v2";

    private static final int BG = Color.rgb(9, 11, 18);
    private static final int PANEL = Color.rgb(20, 23, 35);
    private static final int PANEL_2 = Color.rgb(29, 33, 49);
    private static final int TEXT = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(150, 158, 183);
    private static final int ACCENT = Color.rgb(124, 92, 255);
    private static final int CYAN = Color.rgb(45, 226, 230);
    private static final int PINK = Color.rgb(255, 93, 162);
    private static final int GOLD = Color.rgb(255, 209, 102);
    private static final int GREEN = Color.rgb(77, 229, 151);
    private static final int KNOT = Color.rgb(83, 89, 111);
    private static final int[] TILE_COLORS = {0, ACCENT, CYAN, PINK, GOLD, GREEN, KNOT};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SharedPreferences prefs;
    private final ToneGenerator tone;
    private final Random fxRandom = new Random();
    private final ArrayList<Particle> particles = new ArrayList<Particle>();

    private final RectF boardRect = new RectF();
    private final RectF[] trayRects = {new RectF(), new RectF(), new RectF()};
    private final RectF rotateRect = new RectF();
    private final RectF shiftRect = new RectF();
    private final RectF pulseRect = new RectF();
    private final RectF menuRect = new RectF();
    private final RectF primaryRect = new RectF();
    private final RectF secondaryRect = new RectF();
    private final RectF helpRect = new RectF();

    private int screen = SCREEN_MENU;
    private GameEngine engine;
    private int selectedPiece = 0;
    private boolean pulseMode = false;
    private boolean showGameOver = false;
    private float density;
    private float cellSize;
    private long lastFrame = SystemClock.uptimeMillis();
    private float shake = 0f;
    private float flash = 0f;
    private String toastText = "";
    private long toastUntil = 0L;

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        density = getResources().getDisplayMetrics().density;
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ToneGenerator tg;
        try { tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 28); }
        catch (RuntimeException ex) { tg = null; }
        tone = tg;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(2));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        float dt = Math.min(0.05f, Math.max(0f, (now - lastFrame) / 1000f));
        lastFrame = now;
        updateFx(dt);

        drawBackground(canvas);
        if (screen == SCREEN_MENU) drawMenu(canvas);
        else if (screen == SCREEN_HELP) drawHelp(canvas);
        else drawGame(canvas);
        drawParticles(canvas);
        drawToast(canvas, now);

        if (!particles.isEmpty() || shake > 0.01f || flash > 0.01f || now < toastUntil) postInvalidateOnAnimation();
    }

    private void drawBackground(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(), Color.rgb(8,10,17), Color.rgb(16,13,30), Shader.TileMode.CLAMP));
        c.drawRect(0,0,getWidth(),getHeight(),paint);
        paint.setShader(null);
        paint.setColor(Color.argb(28,124,92,255));
        c.drawCircle(getWidth()*0.15f, getHeight()*0.18f, getWidth()*0.48f, paint);
        paint.setColor(Color.argb(18,45,226,230));
        c.drawCircle(getWidth()*0.85f, getHeight()*0.66f, getWidth()*0.45f, paint);
    }

    private void drawMenu(Canvas c) {
        float w = getWidth();
        float top = dp(62);
        drawLogo(c, w/2f, top + dp(42), dp(78));
        text(c, "CHAINLOOM", w/2f, top + dp(118), dp(34), TEXT, Paint.Align.CENTER, true);
        text(c, "PLACE  •  CONNECT  •  CASCADE", w/2f, top + dp(145), dp(12), MUTED, Paint.Align.CENTER, true);

        float bw = Math.min(w - dp(48), dp(360));
        float bx = (w-bw)/2f;
        float y = top + dp(205);
        primaryRect.set(bx,y,bx+bw,y+dp(62));
        drawButton(c, primaryRect, "PLAY", ACCENT, TEXT, true);

        y += dp(76);
        secondaryRect.set(bx,y,bx+bw,y+dp(58));
        drawButton(c, secondaryRect, "DAILY LOOM", PANEL_2, TEXT, false);

        y += dp(70);
        helpRect.set(bx,y,bx+bw,y+dp(54));
        drawButton(c, helpRect, "HOW TO PLAY", PANEL, MUTED, false);

        String saved = prefs.getString(SAVE_KEY, null);
        if (saved != null && GameEngine.importState(saved) != null) {
            y += dp(66);
            menuRect.set(bx,y,bx+bw,y+dp(48));
            drawButton(c, menuRect, "RESUME RUN", Color.argb(80,45,226,230), CYAN, false);
        } else menuRect.setEmpty();

        text(c, "No forced ads. No energy timer. Offline.", w/2f, getHeight()-dp(36), dp(12), MUTED, Paint.Align.CENTER, false);
    }

    private void drawLogo(Canvas c, float cx, float cy, float size) {
        float s = size/2.4f, gap = size*0.08f;
        int[] colors = {ACCENT,CYAN,PINK,GOLD};
        for (int i=0;i<4;i++) {
            int row=i/2, col=i%2;
            float x=cx-size/2f+col*(s+gap), y=cy-size/2f+row*(s+gap);
            paint.setColor(colors[i]);
            c.drawRoundRect(new RectF(x,y,x+s,y+s), s*0.22f,s*0.22f,paint);
        }
        paint.setColor(TEXT);
        c.drawRoundRect(new RectF(cx-size*0.09f,cy-size*0.31f,cx+size*0.09f,cy+size*0.31f),dp(4),dp(4),paint);
    }

    private void drawHelp(Canvas c) {
        float m=dp(24), w=getWidth()-m*2;
        text(c,"HOW TO PLAY",m,dp(60),dp(28),TEXT,Paint.Align.LEFT,true);
        text(c,"A block puzzle with color-chain tactics.",m,dp(90),dp(14),MUTED,Paint.Align.LEFT,false);
        float y=dp(132);
        y=helpCard(c,m,y,w,"1", "PLACE", "Pick one of three pieces. Rotate it, then tap the board to place it.", ACCENT);
        y=helpCard(c,m,y+dp(12),w,"2", "CONNECT", "A full row/column clears. Matching color groups clear at 6+; the target rises in long runs.", CYAN);
        y=helpCard(c,m,y+dp(12),w,"3", "CASCADE", "Clears collapse downward and can trigger another chain for bigger scores.", PINK);
        y=helpCard(c,m,y+dp(12),w,"4", "SURVIVE", "Gray Knots begin appearing later. Clear them in lines, or spend a full Pulse.", GOLD);
        primaryRect.set(m,getHeight()-dp(84),getWidth()-m,getHeight()-dp(28));
        drawButton(c,primaryRect,"GOT IT",ACCENT,TEXT,true);
    }

    private float helpCard(Canvas c,float x,float y,float w,String n,String title,String body,int color) {
        RectF r=new RectF(x,y,x+w,y+dp(84));
        paint.setColor(PANEL); c.drawRoundRect(r,dp(18),dp(18),paint);
        paint.setColor(color); c.drawCircle(x+dp(28),y+dp(26),dp(14),paint);
        text(c,n,x+dp(28),y+dp(31),dp(12),BG,Paint.Align.CENTER,true);
        text(c,title,x+dp(54),y+dp(26),dp(14),TEXT,Paint.Align.LEFT,true);
        drawWrapped(c,body,x+dp(54),y+dp(47),w-dp(72),dp(12),MUTED,dp(15));
        return y+dp(84);
    }

    private void drawGame(Canvas c) {
        if (engine == null) { startNew(false); return; }
        float w=getWidth(), h=getHeight();
        float ox = shake > 0 ? (fxRandom.nextFloat()*2f-1f)*shake*dp(5) : 0f;
        c.save(); c.translate(ox,0);

        text(c, engine.getMode()==GameEngine.MODE_DAILY ? "DAILY LOOM" : "CHAINLOOM", dp(18), dp(35), dp(18), TEXT, Paint.Align.LEFT, true);
        text(c, "MENU", w-dp(18), dp(35), dp(12), MUTED, Paint.Align.RIGHT, true);
        menuRect.set(w-dp(78),dp(10),w-dp(8),dp(50));

        int best = currentBest();
        text(c, compact(engine.getScore()), dp(18), dp(78), dp(30), TEXT, Paint.Align.LEFT, true);
        text(c, "SCORE", dp(18), dp(97), dp(10), MUTED, Paint.Align.LEFT, true);
        text(c, compact(best), w/2f, dp(78), dp(18), CYAN, Paint.Align.CENTER, true);
        text(c, "BEST", w/2f, dp(97), dp(10), MUTED, Paint.Align.CENTER, true);
        String combo = engine.getCombo()>1 ? "×"+engine.getCombo() : "—";
        text(c, combo, w-dp(18), dp(78), dp(22), PINK, Paint.Align.RIGHT, true);
        text(c, "CHAIN", w-dp(18), dp(97), dp(10), MUTED, Paint.Align.RIGHT, true);

        float maxBoard = Math.min(w-dp(28), h*0.475f);
        float boardSize = Math.min(maxBoard, dp(440));
        float bx=(w-boardSize)/2f, by=dp(116);
        boardRect.set(bx,by,bx+boardSize,by+boardSize);
        cellSize=boardSize/GameEngine.SIZE;
        drawBoard(c);

        float trayY=boardRect.bottom+dp(15);
        float gap=dp(8), margin=dp(14), trayW=(w-margin*2-gap*2)/3f, trayH=dp(88);
        for(int i=0;i<3;i++) {
            trayRects[i].set(margin+i*(trayW+gap),trayY,margin+i*(trayW+gap)+trayW,trayY+trayH);
            drawTrayPiece(c,i,trayRects[i]);
        }

        float controlY=trayY+trayH+dp(12), controlGap=dp(8), controlW=(w-margin*2-controlGap*2)/3f;
        rotateRect.set(margin,controlY,margin+controlW,controlY+dp(52));
        shiftRect.set(margin+controlW+controlGap,controlY,margin+controlW*2+controlGap,controlY+dp(52));
        pulseRect.set(margin+(controlW+controlGap)*2,controlY,w-margin,controlY+dp(52));
        drawButton(c,rotateRect,"↻ ROTATE",PANEL_2,TEXT,false);
        drawButton(c,shiftRect,"SHIFT  "+engine.getShifts(), engine.getShifts()>0?PANEL_2:PANEL, engine.getShifts()>0?TEXT:MUTED,false);
        int pc=engine.isPulseReady()?CYAN:PANEL_2;
        String pl=engine.isPulseReady()?(pulseMode?"TAP TILE":"PULSE"):("PULSE "+engine.getPulse()+"%");
        drawButton(c,pulseRect,pl,pc,engine.isPulseReady()?BG:MUTED,engine.isPulseReady());

        if(engine.isStuckButRescuable()) {
            text(c,"NO PIECE FITS — USE SHIFT OR PULSE",w/2f,controlY+dp(76),dp(12),GOLD,Paint.Align.CENTER,true);
        } else {
            text(c,"Rows • columns • matching color chains",w/2f,controlY+dp(76),dp(11),MUTED,Paint.Align.CENTER,false);
        }

        c.restore();

        if (flash>0) {
            paint.setColor(Color.argb((int)(Math.min(1f,flash)*70),255,255,255));
            c.drawRect(0,0,w,h,paint);
        }

        if (engine.isGameOver() || showGameOver) drawGameOver(c);
    }

    private void drawBoard(Canvas c) {
        paint.setColor(Color.argb(205,18,21,32));
        c.drawRoundRect(boardRect,dp(18),dp(18),paint);
        float pad=dp(5), gap=dp(3);
        for(int r=0;r<GameEngine.SIZE;r++) for(int col=0;col<GameEngine.SIZE;col++) {
            float x=boardRect.left+col*cellSize+gap/2f;
            float y=boardRect.top+r*cellSize+gap/2f;
            RectF cr=new RectF(x,y,x+cellSize-gap,y+cellSize-gap);
            int v=engine.getCell(r,col);
            if(v==0) {
                paint.setColor(Color.rgb(27,30,44));
                c.drawRoundRect(cr,dp(6),dp(6),paint);
            } else {
                paint.setColor(TILE_COLORS[Math.min(v,TILE_COLORS.length-1)]);
                c.drawRoundRect(cr,dp(7),dp(7),paint);
                paint.setColor(Color.argb(65,255,255,255));
                c.drawRoundRect(new RectF(cr.left+dp(2),cr.top+dp(2),cr.right-dp(2),cr.top+cr.height()*0.34f),dp(5),dp(5),paint);
                if(v==GameEngine.KNOT) {
                    stroke.setColor(Color.rgb(168,174,195)); stroke.setStrokeWidth(dp(2.2f));
                    c.drawLine(cr.left+cr.width()*0.3f,cr.top+cr.height()*0.3f,cr.right-cr.width()*0.3f,cr.bottom-cr.height()*0.3f,stroke);
                    c.drawLine(cr.right-cr.width()*0.3f,cr.top+cr.height()*0.3f,cr.left+cr.width()*0.3f,cr.bottom-cr.height()*0.3f,stroke);
                }
            }
        }
    }

    private void drawTrayPiece(Canvas c,int index,RectF rect) {
        GameEngine.Piece p=engine.getPiece(index);
        paint.setColor(index==selectedPiece?Color.rgb(37,35,63):PANEL);
        c.drawRoundRect(rect,dp(15),dp(15),paint);
        if(index==selectedPiece) {
            stroke.setColor(ACCENT); stroke.setStrokeWidth(dp(2)); c.drawRoundRect(rect,dp(15),dp(15),stroke);
        }
        if(p==null) { text(c,"USED",rect.centerX(),rect.centerY()+dp(4),dp(10),MUTED,Paint.Align.CENTER,true); return; }
        int[][] cells=p.cells(); int maxR=0,maxC=0;
        for(int[] q:cells){maxR=Math.max(maxR,q[0]);maxC=Math.max(maxC,q[1]);}
        float unit=Math.min((rect.width()-dp(24))/(maxC+1),(rect.height()-dp(22))/(maxR+1));
        unit=Math.min(unit,dp(24));
        float totalW=(maxC+1)*unit,totalH=(maxR+1)*unit;
        float sx=rect.centerX()-totalW/2f,sy=rect.centerY()-totalH/2f;
        for(int[] q:cells){
            RectF rr=new RectF(sx+q[1]*unit+dp(1),sy+q[0]*unit+dp(1),sx+(q[1]+1)*unit-dp(1),sy+(q[0]+1)*unit-dp(1));
            paint.setColor(TILE_COLORS[p.color]); c.drawRoundRect(rr,dp(4),dp(4),paint);
        }
    }

    private void drawGameOver(Canvas c) {
        updateBest();
        paint.setColor(Color.argb(220,7,9,15)); c.drawRect(0,0,getWidth(),getHeight(),paint);
        float w=getWidth();
        text(c,"LOOM ENDED",w/2f,getHeight()*0.30f,dp(14),PINK,Paint.Align.CENTER,true);
        text(c,compact(engine.getScore()),w/2f,getHeight()*0.39f,dp(52),TEXT,Paint.Align.CENTER,true);
        text(c,"BEST  "+compact(currentBest()),w/2f,getHeight()*0.435f,dp(13),CYAN,Paint.Align.CENTER,true);
        text(c,"Longest chain ×"+Math.max(1,engine.getBestCombo()),w/2f,getHeight()*0.47f,dp(12),MUTED,Paint.Align.CENTER,false);
        float m=dp(30),y=getHeight()*0.55f;
        primaryRect.set(m,y,w-m,y+dp(60)); drawButton(c,primaryRect,"PLAY AGAIN",ACCENT,TEXT,true);
        secondaryRect.set(m,y+dp(74),w-m,y+dp(128)); drawButton(c,secondaryRect,"MAIN MENU",PANEL_2,TEXT,false);
    }

    private void drawButton(Canvas c,RectF r,String label,int bg,int fg,boolean bold) {
        paint.setColor(bg); c.drawRoundRect(r,dp(15),dp(15),paint);
        text(c,label,r.centerX(),r.centerY()+dp(5),dp(13),fg,Paint.Align.CENTER,bold);
    }

    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align,boolean bold) {
        paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size); paint.setTextAlign(align);
        paint.setTypeface(bold?android.graphics.Typeface.DEFAULT_BOLD:android.graphics.Typeface.DEFAULT);
        c.drawText(s,x,y,paint);
    }

    private void drawWrapped(Canvas c,String s,float x,float y,float maxW,float size,int color,float lineHeight) {
        String[] words=s.split(" "); StringBuilder line=new StringBuilder();
        for(String word:words){
            String test=line.length()==0?word:line+" "+word;
            paint.setTextSize(size); paint.setTypeface(android.graphics.Typeface.DEFAULT);
            if(paint.measureText(test)>maxW && line.length()>0){ text(c,line.toString(),x,y,size,color,Paint.Align.LEFT,false); y+=lineHeight; line.setLength(0); line.append(word); }
            else { if(line.length()>0) line.append(' '); line.append(word); }
        }
        if(line.length()>0) text(c,line.toString(),x,y,size,color,Paint.Align.LEFT,false);
    }

    private void drawParticles(Canvas c) {
        for(Particle p:particles){
            paint.setColor(Color.argb((int)(255*Math.max(0,p.life)), Color.red(p.color),Color.green(p.color),Color.blue(p.color)));
            c.drawCircle(p.x,p.y,p.radius*Math.max(0.2f,p.life),paint);
        }
    }

    private void updateFx(float dt) {
        shake=Math.max(0,shake-dt*5f); flash=Math.max(0,flash-dt*3.6f);
        for(int i=particles.size()-1;i>=0;i--){
            Particle p=particles.get(i); p.life-=dt*1.9f; p.x+=p.vx*dt; p.y+=p.vy*dt; p.vy+=dp(85)*dt;
            if(p.life<=0) particles.remove(i);
        }
    }

    private void spawnClearFx(int count,int cascades) {
        int n=Math.min(80,12+count*2+cascades*8);
        for(int i=0;i<n;i++){
            Particle p=new Particle(); p.x=boardRect.centerX()+(fxRandom.nextFloat()-0.5f)*boardRect.width()*0.75f;
            p.y=boardRect.centerY()+(fxRandom.nextFloat()-0.5f)*boardRect.height()*0.65f;
            p.vx=(fxRandom.nextFloat()-0.5f)*dp(180); p.vy=-fxRandom.nextFloat()*dp(180)-dp(20); p.life=0.7f+fxRandom.nextFloat()*0.3f;
            p.radius=dp(2.5f+fxRandom.nextFloat()*3f); p.color=TILE_COLORS[1+fxRandom.nextInt(5)]; particles.add(p);
        }
        flash=Math.min(1f,0.32f+cascades*0.12f); shake=Math.min(1f,0.25f+cascades*0.18f);
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        if(tone!=null) tone.startTone(cascades>1?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,60+cascades*25);
    }

    private void drawToast(Canvas c,long now) {
        if(now>=toastUntil || toastText.length()==0) return;
        float tw=Math.min(getWidth()-dp(36),paint.measureText(toastText)+dp(42));
        RectF r=new RectF((getWidth()-tw)/2f,getHeight()-dp(74),(getWidth()+tw)/2f,getHeight()-dp(30));
        paint.setColor(Color.argb(235,31,35,51)); c.drawRoundRect(r,dp(14),dp(14),paint);
        text(c,toastText,r.centerX(),r.centerY()+dp(5),dp(12),TEXT,Paint.Align.CENTER,true);
    }

    private void toast(String s){toastText=s;toastUntil=SystemClock.uptimeMillis()+1300;invalidate();}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()!=MotionEvent.ACTION_UP) return true;
        float x=event.getX(),y=event.getY();
        if(screen==SCREEN_MENU){
            if(primaryRect.contains(x,y)) startNew(false);
            else if(secondaryRect.contains(x,y)) startNew(true);
            else if(helpRect.contains(x,y)){screen=SCREEN_HELP;invalidate();}
            else if(!menuRect.isEmpty()&&menuRect.contains(x,y)) resumeSaved();
            return true;
        }
        if(screen==SCREEN_HELP){ if(primaryRect.contains(x,y)){screen=SCREEN_MENU;invalidate();} return true; }
        if(showGameOver || (engine!=null&&engine.isGameOver())){
            if(primaryRect.contains(x,y)) startNew(engine!=null&&engine.getMode()==GameEngine.MODE_DAILY);
            else if(secondaryRect.contains(x,y)){clearSave();screen=SCREEN_MENU;showGameOver=false;invalidate();}
            return true;
        }
        if(menuRect.contains(x,y)){persistSession();screen=SCREEN_MENU;invalidate();return true;}
        for(int i=0;i<3;i++) if(trayRects[i].contains(x,y)&&engine.getPiece(i)!=null){selectedPiece=i;pulseMode=false;invalidate();return true;}
        if(rotateRect.contains(x,y)){ if(engine.rotatePiece(selectedPiece)!=null){pulseMode=false;tapFeedback();} invalidate();return true; }
        if(shiftRect.contains(x,y)){
            if(engine.shiftTray()){selectedPiece=firstPiece();pulseMode=false;toast("Tray shifted");tapFeedback();}
            else toast("No Shift tokens");
            finishIfNeeded(); invalidate(); return true;
        }
        if(pulseRect.contains(x,y)){
            if(engine.isPulseReady()){pulseMode=!pulseMode;toast(pulseMode?"Tap an occupied tile":"Pulse cancelled");tapFeedback();}
            else toast("Clear tiles to charge Pulse");
            invalidate();return true;
        }
        if(boardRect.contains(x,y)){
            int col=(int)((x-boardRect.left)/cellSize),row=(int)((y-boardRect.top)/cellSize);
            if(row<0||row>=8||col<0||col>=8) return true;
            if(pulseMode&&engine.isPulseReady()){
                GameEngine.ActionResult ar=engine.usePulse(row,col);
                if(ar.success){pulseMode=false;spawnClearFx(ar.cleared,ar.cascades); if(ar.earnedShift) toast("Shift earned");}
                else badFeedback();
            } else {
                GameEngine.ActionResult ar=engine.place(selectedPiece,row,col);
                if(ar.success){
                    if(ar.cleared>0) spawnClearFx(ar.cleared,ar.cascades); else tapFeedback();
                    if(ar.earnedShift) toast("Shift earned");
                    if(engine.getPiece(selectedPiece)==null) selectedPiece=firstPiece();
                } else badFeedback();
            }
            finishIfNeeded(); persistSession(); invalidate(); return true;
        }
        return true;
    }

    private void tapFeedback(){performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);if(tone!=null)tone.startTone(ToneGenerator.TONE_PROP_BEEP,25);}
    private void badFeedback(){shake=0.55f;performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);toast("That piece doesn’t fit there");}

    private void finishIfNeeded(){
        if(engine!=null&&engine.isGameOver()){showGameOver=true;updateBest();clearSave();if(tone!=null)tone.startTone(ToneGenerator.TONE_PROP_NACK,180);}
    }

    private void startNew(boolean daily){
        long seed=daily?dailySeed():(System.nanoTime()^System.currentTimeMillis());
        engine=new GameEngine(seed,daily?GameEngine.MODE_DAILY:GameEngine.MODE_NORMAL);
        selectedPiece=firstPiece();pulseMode=false;showGameOver=false;screen=SCREEN_GAME;clearSave();invalidate();
    }

    private void resumeSaved(){
        GameEngine restored=GameEngine.importState(prefs.getString(SAVE_KEY,null));
        if(restored==null){clearSave();toast("Saved run could not be restored");return;}
        engine=restored;selectedPiece=firstPiece();pulseMode=false;showGameOver=engine.isGameOver();screen=SCREEN_GAME;invalidate();
    }

    private int firstPiece(){ if(engine==null)return 0;for(int i=0;i<3;i++)if(engine.getPiece(i)!=null)return i;return 0; }

    public void persistSession(){
        if(screen==SCREEN_GAME&&engine!=null&&!engine.isGameOver()) prefs.edit().putString(SAVE_KEY,engine.exportState()).apply();
    }
    private void clearSave(){prefs.edit().remove(SAVE_KEY).apply();}

    private void updateBest(){
        if(engine==null)return;
        if(engine.getMode()==GameEngine.MODE_DAILY){
            String key="daily_best_"+dailyKeyForEngine(); int old=prefs.getInt(key,0); if(engine.getScore()>old)prefs.edit().putInt(key,engine.getScore()).apply();
        }else{int old=prefs.getInt("best_normal",0);if(engine.getScore()>old)prefs.edit().putInt("best_normal",engine.getScore()).apply();}
    }
    private int currentBest(){
        if(engine==null)return prefs.getInt("best_normal",0);
        return engine.getMode()==GameEngine.MODE_DAILY?prefs.getInt("daily_best_"+dailyKeyForEngine(),0):prefs.getInt("best_normal",0);
    }

    private long dailySeed(){ return DAILY_SEED_SALT ^ Long.parseLong(dailyKey()); }
    private String dailyKeyForEngine(){
        if(engine==null || engine.getMode()!=GameEngine.MODE_DAILY) return dailyKey();
        long encoded = engine.getSeed() ^ DAILY_SEED_SALT;
        return String.format(Locale.US,"%07d",encoded);
    }
    private String dailyKey(){ Calendar cal=Calendar.getInstance(TimeZone.getTimeZone("UTC")); return String.format(Locale.US,"%04d%03d",cal.get(Calendar.YEAR),cal.get(Calendar.DAY_OF_YEAR)); }

    public boolean handleBack(){
        if(screen==SCREEN_HELP){screen=SCREEN_MENU;invalidate();return true;}
        if(screen==SCREEN_GAME){persistSession();screen=SCREEN_MENU;showGameOver=false;invalidate();return true;}
        return false;
    }

    public void release(){ if(tone!=null) tone.release(); }

    private float dp(float v){return v*density;}
    private String compact(int n){if(n<1000)return String.valueOf(n);if(n<1000000)return String.format(Locale.US,"%.1fK",n/1000f);return String.format(Locale.US,"%.1fM",n/1000000f);}

    private static final class Particle {float x,y,vx,vy,life,radius;int color;}
}
