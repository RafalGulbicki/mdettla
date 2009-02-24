/**
   @version 1.10
   @author Micha� Dettlaff
 */

/* <applet width=490 height=385 code="MazeGenSolve.class"></applet> */

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;
import java.util.Date;
 
/**
 * Generuje labirynt przy pomocy algorytmu Depth-First Search,
 * a nastepnie rozwiazuje go przy pomocy algorytmu Tremaux.
 * Predkosc rysowania kontroluja klawisze kursorow.
 * Wcisniecie spacji pauzuje animacje.
 * Klawisz 'G' wylacza i wlacza animacje generowania labiryntu.
 * Klawisz 'S' wylacza i wlacza animacje rozwiazywania labiryntu.
 * Klawisz 'B' chowa i pokazuje backtrack.
 */
public class MazeGenSolve extends Applet
  implements Runnable, MouseListener, KeyListener {
 
  final int MAX_DRAWING_SPEED=5; // najwieksza predkosc rysowania
  final int MIN_DRAWING_SPEED=1000; // najmniejsza predkosc rysowania
  final int CELL_SIZE=15; // wielkosc komorki labiryntu w pikselach
  final int X_MAR=5; // margines boczny
  final int Y_MAR=5; // margines gorny i dolny
  // pozycje sasiadow komorki (polnoc, poludnie, wschod, zachod)
  final Coords[] DIRECTIONS = {new Coords(0,-1), new Coords(0,1),
			   new Coords(1,0), new Coords(-1,0)};
  Thread t = null;
  boolean threadSuspended;
  Graphics buffer; // grafike bedziemy rysowac w buforze, zeby nie migalo
  Image bufferImage; // pojedyncza klatka, do ktorej wrzucamy gotowy bufor
  boolean generateNewMazeNow=false;
  boolean showGen=true;
  boolean showSolve=true;
  boolean showBacktrack=true;
  int drawingSpeed=25; // interval given in milliseconds
  int width, height; // rozmiary appletu
  int i;
  int mazeWidth=32; // szerokosc labiryntu w komorkach
  int mazeHeight=25; // wysokosc labiryntu w komorkach
  Maze maze;

  // Metoda wykonuje sie przy uruchomieniu appleta.
  public void init() {
    // szerokosc i wysokosc appletu
    width = getSize().width;
    height = getSize().height;
    setBackground(new Color(64, 64, 64)); // ciemnoszary

    bufferImage = createImage(width, height);
    buffer = bufferImage.getGraphics();

    addMouseListener(this);
    addKeyListener(this);
  }

  public void destroy() {
    removeMouseListener(this);
    removeKeyListener(this);
  }

  // Executed after the applet is created; and also whenever
  // the browser returns to the page containing the applet.
  public void start() {
    if (t == null) {
      t = new Thread(this);
      threadSuspended = false;
      t.start();
    }
    else {
      if (threadSuspended) {
	threadSuspended = false;
	synchronized(this) {
	  notify();
	}
      }
    }
  }

  // Executed whenever the browser leaves the page containing the applet.
  public void stop() {
    threadSuspended=true;
  }

  // Executed within the thread that this applet created.
  public void run() {
    Date date = new Date(); // czas uniksowy uzyjemy jako seed w rand
    Random rand = new Random(date.getTime()); // generator liczb pseudolosowych
    while (true) {
      maze = new Maze(mazeWidth, mazeHeight);

      if (showGen && !generateNewMazeNow)
	paintBuffer();
      delay(500);

      generateMazeDFS(rand);

      if (!showGen || generateNewMazeNow)
	paintBuffer();
      if (generateNewMazeNow) {
	generateNewMazeNow=false;
	delay(2000);
      }
      delay(1000);
      addEntryAndExit(rand);
      if (!generateNewMazeNow)
	paintBuffer();
      delay(1000);

      solveMazeTremaux(rand);

      if (!showSolve && !generateNewMazeNow)
	paintBuffer();
      delay(3000);
    }
  }

  /** Przygotowuje bufor i wyswietla go. */
  void paintBuffer() {
    drawInBuffer(); // rysuje labirynt w buforze
    repaint(); // wyswietla bufor
  }

  /** Czeka zadana ilosc milisekund. */
  void delay(int interval) {
    try {
      if (interval >= 100) { // dzielimy interval na kawalki
	for (i=0; i < interval / 10 && !generateNewMazeNow; i++) {
	  t.sleep(10); // interval given in milliseconds
	  // Now the thread checks to see if it should suspend itself
	  if (threadSuspended) {
	    synchronized(this) {
	      while (threadSuspended) {
		wait();
	      }
	    }
	  }
	}
      }
      else {
	if (!generateNewMazeNow) {
	  t.sleep(interval); // interval given in milliseconds
	  // Now the thread checks to see if it should suspend itself
	  if (threadSuspended) {
	    synchronized(this) {
	      while (threadSuspended) {
		wait();
	      }
	    }
	  }
	}
      }
    }
    catch (InterruptedException e) { }
  }

  /**
   * Buduje labirynt przez sukcesywne burzenie scian, stosujac algorytm
   * Depth-First Search generowania labiryntu.
   * Algorytm tworzy labirynt typu perfect maze, tzn. taki, w ktorym
   * istnieje dokladnie jedna sciezka miedzy kazdymi dwoma pozycjami.
   */
  void generateMazeDFS(Random rand) {
    MyStack<Coords> cellStack = new MyStack<Coords>();
    int totalCells = mazeWidth * mazeHeight;
    Coords currentCell = new Coords(rand.nextInt(mazeWidth),
				rand.nextInt(mazeHeight));
    Coords newCell;
    int visitedCells = 1;
    // ArrayList to tablica o zmiennej dlugosci z interfejsem listy
    ArrayList<Coords> intactNeighbors;
    Coords nextNeighbor;

    while (visitedCells < totalCells) {
      // find all neighbors of currentCell with all walls intact
      intactNeighbors = new ArrayList<Coords>(); // wyczyszczenie tablicy
      for (int i=0; i < DIRECTIONS.length; i++) { // kolejne kierunki
	nextNeighbor = Coords.add(currentCell, DIRECTIONS[i]);
	if (!maze.getCell(currentCell).isBorderInDirection(DIRECTIONS[i]))
	  if (maze.getCell(nextNeighbor).isIntact())
	    intactNeighbors.add(nextNeighbor);
      }
      if (intactNeighbors.size() > 0) { // if one or more found 
	// choose one at random
	newCell = intactNeighbors.get(rand.nextInt(intactNeighbors.size()));
	maze.knockDownWall(newCell, currentCell);

	// obsluga animacji ilustrujacej dzialanie algorytmu
	if (showGen && !generateNewMazeNow) {
	  delay(drawingSpeed);
	  paintBuffer();
	}

	cellStack.push(currentCell);
	currentCell=newCell;
	visitedCells++;
      }
      else 
	currentCell=cellStack.pop();
    }
  }

  /** Umieszcza wejscie i wyjscie z labiryntu. */
  void addEntryAndExit(Random rand) {
    switch (rand.nextInt(4)) {
      case 0: 
	maze.setEntry(0, mazeHeight-1); // wspolrzedne wejscia
	maze.setExit(mazeWidth-1, 0); // wspolrzedne wyjscia
	break;
      case 1: 
	maze.setEntry(mazeWidth-1, 0);
	maze.setExit(0, mazeHeight-1);
	break;
      case 2: 
	maze.setEntry(0, 0);
	maze.setExit(mazeWidth-1, mazeHeight-1);
	break;
      case 3: 
	maze.setEntry(mazeWidth-1, mazeHeight-1);
	maze.setExit(0, 0);
	break;
    }
  }

  /**
   * Algorytm Tremaux znajdujacy wyjscie z labiryntu.
   * Znajdzie rozwiazanie dla dowolnego labiryntu, ale niekoniecznie
   * najkrotsze. Po zakonczeniu algorytmu albo znajdziemy wyjscie (true),
   * albo przejdziemy caly labirynt i wrocimy na poczatek (false).
   */
  boolean solveMazeTremaux(Random rand) {
    Coords currentCell = maze.getEntry();
    Coords newCell;
    ArrayList<Coords> freeNeighbors;
    ArrayList<Coords> neighborsWithSolution;
    Coords nextNeighbor;

    while (true) {
      // znajdujemy dostepnych sasiadow bez bactrack i solution
      // oraz dostepnych sasiadow bez backtrack, ale z solution
      freeNeighbors = new ArrayList<Coords>();
      neighborsWithSolution = new ArrayList<Coords>();
      for (int i=0; i < DIRECTIONS.length; i++) {
	nextNeighbor = Coords.add(currentCell, DIRECTIONS[i]);
	if (!maze.getCell(currentCell).isBorderInDirection(DIRECTIONS[i])) {
	  if (!maze.getCell(currentCell).isWallInDirection(DIRECTIONS[i])
	      && !maze.getCell(nextNeighbor).isBacktrackIn()
	      && !maze.getCell(nextNeighbor).isSolutionIn())
	    freeNeighbors.add(nextNeighbor);
	  else if (!maze.getCell(currentCell).isWallInDirection(DIRECTIONS[i])
	      && !maze.getCell(nextNeighbor).isBacktrackIn())
	    neighborsWithSolution.add(nextNeighbor);
	}
      }
      // jesli doszlismy do wyjscia z labiryntu, koniec
      if (currentCell.equals(maze.getExit()))
	return true;
      else if (freeNeighbors.size() > 0) { // jesli obok sa wolne komorki
	// wybieramy jedna losowo i idziemy w jej kierunku znaczac solution
	newCell = freeNeighbors.get(rand.nextInt(freeNeighbors.size()));
	maze.getCell(currentCell).markSolution(currentCell, newCell);
	currentCell = newCell;
      }
      // jesli jest obok komorka z solution do ktorej mozna pojsc
      else if (neighborsWithSolution.size() > 0) {
	// idziemy tam znaczac po drodze backtrack
	// i odznaczajac solution nadchodzace z naprzeciwka
	newCell = neighborsWithSolution.get(rand.nextInt(
					  neighborsWithSolution.size()));
	maze.getCell(currentCell).markBacktrack(currentCell, newCell);
	maze.getCell(newCell).unmarkSolution(newCell, currentCell);
	currentCell = newCell;
      }
      else // nie mozna juz isc dalej; wrocilismy na poczatek labiryntu
	return false; // labirynt nie ma wyjscia

      // obsluga animacji ilustrujacej dzialanie algorytmu
      if (showSolve && !generateNewMazeNow) {
	delay(drawingSpeed);
	paintBuffer();
      }
    }
  }

  /** Rysuje labirynt w buforze. Bufor stanowi pojedyncza klatke animacji. */
  void drawInBuffer() {
    final int PADD=6; // padding; grubosc sciezek solution i backtrack
    buffer.clearRect(0, 0, width, height); // czyscimy bufor
    // rysowanie labiryntu, poprzez rysowanie jego kolejnych komorek
    buffer.setColor(Color.white);
    for (i=0; i < mazeWidth; i++)
      for (int j=0; j < mazeHeight; j++) { // rysowanie komorki
        if (maze.getCell(i, j).walls.north) // jesli stoi polnocna sciana
          buffer.drawLine(X_MAR+i*CELL_SIZE, Y_MAR+j*CELL_SIZE,
                 X_MAR+(i+1)*CELL_SIZE, Y_MAR+j*CELL_SIZE);
        if (maze.getCell(i, j).walls.south) // jesli stoi poludniowa sciana
          buffer.drawLine(X_MAR+i*CELL_SIZE, Y_MAR+(j+1)*CELL_SIZE,
                 X_MAR+(i+1)*CELL_SIZE, Y_MAR+(j+1)*CELL_SIZE);
	if (maze.getCell(i, j).walls.east) // jesli stoi wschodnia sciana
	    buffer.drawLine(X_MAR+(i+1)*CELL_SIZE, Y_MAR+j*CELL_SIZE,
		       X_MAR+(i+1)*CELL_SIZE, Y_MAR+(j+1)*CELL_SIZE);
	if (maze.getCell(i, j).walls.west) // jesli stoi zachodnia sciana
	    buffer.drawLine(X_MAR+i*CELL_SIZE, Y_MAR+j*CELL_SIZE,
		       X_MAR+i*CELL_SIZE, Y_MAR+(j+1)*CELL_SIZE);
    }
    // rysowanie backtrack
    if (showBacktrack) {
      buffer.setColor(new Color(96, 96, 255)); // niebieski
      for (i=0; i < mazeWidth; i++) {
	for (int j=0; j < mazeHeight; j++) { // rysowanie w komorce
	  if (maze.getCell(i, j).backtrack.north)
	    buffer.fillRect(X_MAR+i*CELL_SIZE+PADD, Y_MAR+(j-1)*CELL_SIZE+PADD,
			    CELL_SIZE-2*PADD+1, 2*(CELL_SIZE-PADD)+1);
	  if (maze.getCell(i, j).backtrack.south)
	    buffer.fillRect(X_MAR+i*CELL_SIZE+PADD, Y_MAR+j*CELL_SIZE+PADD,
			    CELL_SIZE-2*PADD+1, 2*(CELL_SIZE-PADD)+1);
	  if (maze.getCell(i, j).backtrack.east)
	    buffer.fillRect(X_MAR+i*CELL_SIZE+PADD, Y_MAR+j*CELL_SIZE+PADD,
			    2*(CELL_SIZE-PADD)+1, CELL_SIZE-2*PADD+1);
	  if (maze.getCell(i, j).backtrack.west)
	    buffer.fillRect(X_MAR+(i-1)*CELL_SIZE+PADD, Y_MAR+j*CELL_SIZE+PADD,
			    2*(CELL_SIZE-PADD)+1, CELL_SIZE-2*PADD+1);
	}
      }
    }
    // rysowanie solution
    buffer.setColor(Color.yellow);
    for (i=0; i < mazeWidth; i++) {
      for (int j=0; j < mazeHeight; j++) { // rysowanie w komorce
        if (maze.getCell(i, j).solution.north)
          buffer.fillRect(X_MAR+i*CELL_SIZE+PADD, Y_MAR+(j-1)*CELL_SIZE+PADD,
			  CELL_SIZE-2*PADD+1, 2*(CELL_SIZE-PADD)+1);
        if (maze.getCell(i, j).solution.south)
          buffer.fillRect(X_MAR+i*CELL_SIZE+PADD, Y_MAR+j*CELL_SIZE+PADD,
			  CELL_SIZE-2*PADD+1, 2*(CELL_SIZE-PADD)+1);
        if (maze.getCell(i, j).solution.east)
          buffer.fillRect(X_MAR+i*CELL_SIZE+PADD, Y_MAR+j*CELL_SIZE+PADD,
			  2*(CELL_SIZE-PADD)+1, CELL_SIZE-2*PADD+1);
        if (maze.getCell(i, j).solution.west)
          buffer.fillRect(X_MAR+(i-1)*CELL_SIZE+PADD, Y_MAR+j*CELL_SIZE+PADD,
			  2*(CELL_SIZE-PADD)+1, CELL_SIZE-2*PADD+1);
      }
    }
    // rysowanie wejscia i wyjscia z labiryntu
    if (maze.getEntry() != null) {
      buffer.setColor(Color.green);
      buffer.fillRect(X_MAR+CELL_SIZE*maze.getEntry().x+2,
	  Y_MAR+CELL_SIZE*maze.getEntry().y+2, CELL_SIZE-3, CELL_SIZE-3);
    }
      if (maze.getExit() != null) {
      buffer.setColor(Color.red);
      buffer.fillRect(X_MAR+CELL_SIZE*maze.getExit().x+2,
	  Y_MAR+CELL_SIZE*maze.getExit().y+2, CELL_SIZE-3, CELL_SIZE-3);
    }
  }

  public void update(Graphics g) {
    g.drawImage(bufferImage, 0, 0, this);
    getToolkit().sync(); // ponoc polepsza wyglad w niektorych systemach
  }

  // Metoda wykonywana przy ponownym wyrysowywaniu appleta.
  public void paint(Graphics g) {
    update(g);
  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
  }

  // Pauza po kliknieciu prawym przyciskiem.
  public void mouseReleased(MouseEvent e) {
    if (e.getButton() == e.BUTTON3) {
      if (threadSuspended) {
	threadSuspended = false;
	synchronized(this) {
	  notify();
	}
      }
      else
	threadSuspended = true;
      e.consume();
    }
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  // Kontrola predkosci rysowania przy pomocy kursorow.
  public void keyPressed(KeyEvent e) {
    // drawingSpeed oznacza wielkosc opoznienia (wiecej = wolniej)
    switch (e.getKeyCode()) {
      case KeyEvent.VK_RIGHT:
	drawingSpeed*=0.8;
	if (drawingSpeed < MAX_DRAWING_SPEED)
	  drawingSpeed=MAX_DRAWING_SPEED;
	break;
      case KeyEvent.VK_LEFT:
	drawingSpeed*=1.2;
	if (drawingSpeed > MIN_DRAWING_SPEED)
	  drawingSpeed=MIN_DRAWING_SPEED;
	break;
      case KeyEvent.VK_DOWN:
	drawingSpeed=MIN_DRAWING_SPEED;
	break;
      case KeyEvent.VK_UP:
	drawingSpeed=MAX_DRAWING_SPEED;
	break;
    }
    e.consume();
  }

  public void keyReleased(KeyEvent e) {
  }

  // Kontrola predkosci rysowania przy pomocy klawiszy '+' i '-'.
  // Pauza po wcisnieciu spacji, 'p' lub 'P'.
  public void keyTyped(KeyEvent e) {
    switch (e.getKeyChar()) {
      case ' ':
      case 'p':
      case 'P':
	if (threadSuspended) {
	  threadSuspended = false;
	  synchronized(this) {
	    notify();
	  }
	}
	else
	  threadSuspended = true;
	break;
      // drawingSpeed oznacza wielkosc opoznienia (wiecej = wolniej)
      case '+':
	drawingSpeed*=0.8;
	if (drawingSpeed < MAX_DRAWING_SPEED)
	  drawingSpeed=MAX_DRAWING_SPEED;
	break;
      case '-':
	drawingSpeed*=1.2;
	if (drawingSpeed > MIN_DRAWING_SPEED)
	  drawingSpeed=MIN_DRAWING_SPEED;
	break;
      case 'g':
      case 'G':
	showGen=!showGen;
	break;
      case 's':
      case 'S':
	showSolve=!showSolve;
	break;
      case 'n':
      case 'N':
	generateNewMazeNow=true;
	if (threadSuspended) { // odpauzowujemy jesli trzeba
	  threadSuspended = false;
	  synchronized(this) {
	    notify();
	  }
	}
	break;
      case 'b':
      case 'B':
	showBacktrack=!showBacktrack;
	break;
    }
    e.consume();
  }

  public String getAppletInfo() {
    return "Aplet generuj�cy i rozwi�zuj�cy labirynt.\nPr�dko�� rysowania kontroluj� klawisze kursor�w.\nWci�ni�cie spacji pauzuje animacj�.\nKlawisz 'G' wy��cza i w��cza animacj� generowania labiryntu.\nKlawisz 'S' wy��cza i w��cza animacj� rozwi�zywania labiryntu.\nKlawisz 'B' chowa i pokazuje backtrack.";
  }
}

/**
 * Labirynt reprezentowany przez tablice komorek, wejscie i wyjscie.
 */
class Maze {
  MazeCell[][] maze; // tablica reprezentujaca labirynt w postaci komorek
  Coords entry, exit; // wspolrzedne wejscia i wyjscia

  Maze(int mazeWidth, int mazeHeight) {
    maze = new MazeCell[mazeWidth][mazeHeight];
    // wstawiamy wszedzie sciany
    for (int i=0; i < mazeWidth; i++)
      for (int j=0; j < mazeHeight; j++)
	maze[i][j] = new MazeCell();
    // nastepnie dodajemy brzegi
    for (int i=0; i < mazeWidth; i++) { // gorny i dolny brzeg labiryntu
      maze[i][0].border.north=true;
      maze[i][mazeHeight-1].border.south=true;
    }
    for (int j=0; j < mazeHeight; j++) { // lewy i prawy brzeg labiryntu
      maze[0][j].border.west=true;
      maze[mazeWidth-1][j].border.east=true;
    }
    // ukrywamy wejscie i wyjscie
    entry = exit = null;
  }

  // Zwraca komorke labiryntu o danych wspolrzednych.
  MazeCell getCell(Coords coords) {
    return maze[coords.x][coords.y];
  }

  MazeCell getCell(int x, int y) {
    return maze[x][y];
  }

  Coords getEntry() { return entry; }

  Coords getExit() { return exit; }

  // Ustawia wspolrzedne wejscia do labiryntu
  void setEntry(int x, int y) {
    entry = new Coords(x, y);
  }

  // Ustawia wspolrzedne wyjscia z labiryntu
  void setExit(int x, int y) {
    exit = new Coords(x, y);
  }

  /** Burzy sciane miedzy dwoma komorkami labiryntu. */
  void knockDownWall(Coords newCell, Coords currentCell) {
    if (newCell.y < currentCell.y) { // sasiad na polnosc
      maze[currentCell.x][currentCell.y].walls.north=false;
      maze[newCell.x][newCell.y].walls.south=false;
    }
    else if (newCell.y > currentCell.y) { // poludnie
      maze[currentCell.x][currentCell.y].walls.south=false;
      maze[newCell.x][newCell.y].walls.north=false;
    }
    else if (newCell.x > currentCell.x) { // wschod
      maze[currentCell.x][currentCell.y].walls.east=false;
      maze[newCell.x][newCell.y].walls.west=false;
    }
    else if (newCell.x < currentCell.x) { // zachod
      maze[currentCell.x][currentCell.y].walls.west=false;
      maze[newCell.x][newCell.y].walls.east=false;
    }
  }
}

/**
 * Klasa reprezentujaca komorke labiryntu.
 */
class MazeCell {
  Directions backtrack;
  Directions solution;
  Directions border;
  Directions walls;

  MazeCell () {
    backtrack = new Directions(false, false, false, false);
    solution = new Directions(false, false, false, false);
    border = new Directions(false, false, false, false);
    // na poczatku wszystkie sciany stoja
    walls = new Directions(true, true, true, true);
  }

  /* metody sprawdzajace zawartosc komorki */

  /** Sprawdza, czy wszystkie sciany komorki stoja. */
  boolean isIntact() {
    return (walls.north && walls.south && walls.east && walls.west);
  }

  /** Sprawdza, czy przez komorke przechodzi solution. */
  boolean isSolutionIn() {
    return (solution.north || solution.south
	    || solution.east || solution.west);
  }

  /** Sprawdza, czy przez komorke przechodzi backtrack. */
  boolean isBacktrackIn() {
    return (backtrack.north || backtrack.south
	|| backtrack.east || backtrack.west);
  }

  /** Sprawdza, czy jest sciana z danej strony komorki. */
  boolean isWallInDirection(Coords direction) {
    if (direction.y < 0 && walls.north == true)
      return true; // jest sciana na polnoc
    if (direction.y > 0 && walls.south == true)
      return true; // jest sciana na poludnie
    if (direction.x > 0 && walls.east == true)
      return true; // jest sciana na wschod
    if (direction.x < 0 && walls.west == true)
      return true; // jest sciana na zachod
    return false;
  }

  /** Sprawdza, czy jest brzeg z danej strony komorki. */
  boolean isBorderInDirection(Coords direction) {
    if (direction.y < 0 && border.north == true)
      return true; // jest brzeg na polnoc
    if (direction.y > 0 && border.south == true)
      return true; // jest brzeg na poludnie
    if (direction.x > 0 && border.east == true)
      return true; // jest brzeg na wschod
    if (direction.x < 0 && border.west == true)
      return true; // jest brzeg na zachod
    return false;
  }

  /* metody modyfikujace zawartosc komorki */

  /** Zaznacza solution w kierunku z currentCell do newCell. */
  void markSolution(Coords currentCell, Coords newCell) {
    if (newCell.y < currentCell.y) // sasiad na polnosc
      solution.north=true;
    else if (newCell.y > currentCell.y) // poludnie
      solution.south=true;
    else if (newCell.x > currentCell.x) // wschod
      solution.east=true;
    else if (newCell.x < currentCell.x) // zachod
      solution.west=true;
  }

  /** Odznacza solution w kierunku z currentCell do newCell. */
  void unmarkSolution(Coords currentCell, Coords newCell) {
    if (newCell.y < currentCell.y) // sasiad na polnosc
      solution.north=false;
    else if (newCell.y > currentCell.y) // poludnie
      solution.south=false;
    else if (newCell.x > currentCell.x) // wschod
      solution.east=false;
    else if (newCell.x < currentCell.x) // zachod
      solution.west=false;
  }

  /**
   * Zaznacza backtrack w kierunku z currentCell do newCell.
   * Przy okazji odznacza solution, zeby sie nie pokrywaly.
   */
  void markBacktrack(Coords currentCell, Coords newCell) {
    solution.north=solution.south=solution.east=solution.west=false;
    if (newCell.y < currentCell.y) // sasiad na polnosc
      backtrack.north=true;
    else if (newCell.y > currentCell.y) // poludnie
      backtrack.south=true;
    else if (newCell.x > currentCell.x) // wschod
      backtrack.east=true;
    else if (newCell.x < currentCell.x) // zachod
      backtrack.west=true;
  }
}

/**
 * Klasa reprezentujaca rozne obiekty w komorce labiryntu (MazeCell)
 */
class Directions {
  boolean north;
  boolean south;
  boolean east;
  boolean west;

  Directions(boolean initNorth, boolean initSouth,
	 boolean initEast, boolean initWest) {
    north=initNorth;
    south=initSouth;
    east=initEast;
    west=initWest;
  }
}

/**
 * Stos utworzony przy pomocy listy dowiazaniowej
 * na podstawie Thinking In Java 4th Edition
 */
class MyStack<Coords> {
  private LinkedList<Coords> storage = new LinkedList<Coords>();
  public void push(Coords v) { storage.addFirst(v); }
  public Coords peek() { return storage.getFirst(); }
  public Coords pop() { return storage.removeFirst(); }
  public boolean empty() { return storage.isEmpty(); }
  public String toString() { return storage.toString(); }
}

/**
 * Przechowuje wspolrzedne x oraz y.
 */
class Coords {
  int x, y;

  Coords(int initX, int initY) {
    x=initX;
    y=initY;
  }

  boolean equals(Coords coords) {
    return (coords.x == x && coords.y == y);
  }

  static Coords add(Coords c1, Coords c2) {
    return new Coords(c1.x+c2.x, c1.y+c2.y);
  }
}

