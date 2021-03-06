#!/usr/bin/env python
# -*- coding: UTF-8 -*-

u"""Animacja pokazująca szukanie drogi w labiryncie przez algorytm genetyczny.

Dla każdego pokolenia wybierany jest z niego najlepiej przystosowany osobnik,
który następnie porusza się po labiryncie zgodnie z ruchami określonymi przez
jego genotyp.

"""

from PyQt4 import QtCore, QtGui
import ga
import sys
import random


usage = u"""Użycie: python animation.py PLIK_Z_LABIRYNTEM"""


class AnimationWindow(QtGui.QMainWindow):
    def __init__(self, maze, moves):
        QtGui.QMainWindow.__init__(self)
        self.setGeometry(300, 300, maze.width * Maze.sq_size,
                maze.height * Maze.sq_size + 24)
        self.setWindowTitle('Algorytm genetyczny - animacja')
        self.maze_canvas = Maze(self, maze, moves)
        self.setCentralWidget(self.maze_canvas)
        self.statusbar = self.statusBar()
        self.connect(self.maze_canvas,
                QtCore.SIGNAL('messageToStatusbar(QString)'), self.statusbar,
                QtCore.SLOT('showMessage(QString)'))
        self.maze_canvas.start()
        self.center()

    def center(self):
        screen = QtGui.QDesktopWidget().screenGeometry()
        size = self.geometry()
        self.move((screen.width()-size.width())/2,
                (screen.height()-size.height())/2)


class Maze(QtGui.QFrame):
    u"""Obszar na którym będziemy rysować labirynt, ścieżkę i agenta."""
    speed = 100
    sq_size = 30

    def __init__(self, parent, maze, moves):
        QtGui.QFrame.__init__(self, parent)
        self.timer = QtCore.QBasicTimer()
        self.setFocusPolicy(QtCore.Qt.StrongFocus)
        self.maze = maze # abstrakcyjna reprezentacja labiryntu (ga.Maze)
        self.moves = moves # lista ciągów ruchów
        self.path = [self.maze.start_pos] # kolejne pozycje agenta
        self.path_ended = False
        self.generation_count = 1

    def start(self):
        self.timer.start(Maze.speed, self)

    def paintEvent(self, event):
        painter = QtGui.QPainter(self)
        black = QtGui.QColor(0x000000)
        white = QtGui.QColor(0xFFFFFF)
        grey = QtGui.QColor(0x808080)
        red = QtGui.QColor(0xCC0000)
        green = QtGui.QColor(0x00CC00)
        blue = QtGui.QColor(0x0000FF)

        self.drawMaze(painter, white, black, green, red)
        self.drawSolution(painter, grey, blue)

    def timerEvent(self, event):
        if self.path_ended:
            self.moves.pop(0) # bierzemy kolejny ciąg ruchów
            self.path_ended = False
            if self.moves: # jeśli to ostatnie pokolenie, nie zerujemy ścieżki
                self.generation_count += 1
                self.path = [self.maze.start_pos]
        self.advanceAgent()
        self.update()
        self.emit(QtCore.SIGNAL('messageToStatusbar(QString)'),
                'Pokolenie ' + str(self.generation_count) +
                ', ruch ' + str(len(self.path)-1))

    def drawMaze(self, painter, bg_color, color, start_color, end_color):
        u"""Narysuj sam labirynt, bez rozwiązania."""
        # tło
        painter.fillRect(0, 0, self.maze.width * Maze.sq_size,
                self.maze.height * Maze.sq_size, bg_color)
        # linie
        painter.setPen(color)
        for j in range(self.maze.height + 1):
            painter.drawLine(0, j * Maze.sq_size,
                    self.maze.width * Maze.sq_size, j * Maze.sq_size)
        for i in range(self.maze.width + 1):
            painter.drawLine(i * Maze.sq_size, 0,
                    i * Maze.sq_size, self.maze.height * Maze.sq_size)
        # ściany
        for j in range(self.maze.height):
            for i in range(self.maze.width):
                if self.maze.squares[j][i] == 1:
                    painter.fillRect(i * Maze.sq_size, j * Maze.sq_size,
                            Maze.sq_size, Maze.sq_size, color)
        # pozycja startowa i końcowa
        painter.fillRect(self.maze.start_pos.x * Maze.sq_size + 1,
                self.maze.start_pos.y * Maze.sq_size + 1,
                Maze.sq_size - 1, Maze.sq_size - 1, start_color)
        painter.fillRect(self.maze.end_pos.x * Maze.sq_size + 1,
                self.maze.end_pos.y * Maze.sq_size + 1,
                Maze.sq_size - 1, Maze.sq_size - 1, end_color)

    def drawSolution(self, painter, path_color, agent_color):
        for i in range(1, len(self.path)):
            position = self.path[i]
            if position == self.path[-1]:
                color = agent_color
            else:
                color = path_color
            if position != self.maze.start_pos and \
                    position != self.maze.end_pos:
                painter.fillRect(position.x * Maze.sq_size + 1,
                        position.y * Maze.sq_size + 1,
                        Maze.sq_size - 1, Maze.sq_size - 1, color)

    def advanceAgent(self):
        u"""Przejdź dalej w kierunku wyjścia z labiryntu."""
        if not self.moves:
            self.timer.stop()
        elif not self.moves[0] or self.path[-1] == self.maze.end_pos:
            self.path_ended = True
        else:
            move = self.moves[0].pop(0)
            self.path.append(self.maze.move(self.path[-1], move))


if __name__ == '__main__':
    try:
        if len(sys.argv) > 1:
            app = QtGui.QApplication(sys.argv)

            ga.maze = ga.Maze(sys.argv[1])
            selection = ga.select_proportional
            results = ga.epoch(ga.m, ga.l, ga.p_c, ga.p_m, 1, selection, None)
            moves = [result.phenotype() for result in results]

            animation = AnimationWindow(ga.maze, moves)
            animation.show()

            sys.exit(app.exec_())
        else:
            print __doc__
            print usage
    except IOError:
        print u'Błąd: nie można odnaleźć pliku', sys.argv[1]
        sys.exit(1)

