from __future__ import print_function
from msgame import MSGame
from BayesianNetworkGenerator import gameNetworkGenerator
from msboard import bcolors
from random import randint
import pgmpy.inference as pgmi
import networkx
import numpy
import sys
import pgmpy.inference.EliminationOrder as elor

def autosolver(anchura, altura, numMinas):
    game = MSGame(anchura, altura, numMinas)
    modelo = gameNetworkGenerator(game)

    #We do a random click on the board to start getting probValues from the squares' experiences.
    print("")
    print("△ Board ------------------------------------------------------")
    print("")
    game.print_board()
    board = game.board
    print(board.mine_map)
    posX = randint(0,game.board_width-1)
    posY = randint(0,game.board_width-1)
    game.mover_minas_alrededor(posX,posY)
    game.play_move("click",posX,posY)
    print("△ Move --> click: " + str(posX)+","+str(posY)+"  ---------------------------------------")
    print("")
    game.print_board()
    print(board.mine_map)
    checkedBoxes = []
    reverse = False


    while game.game_status == 2:
        """
        As long as there are cells to be discovered, we go through the board and obtain the already discovered cells and their associated probValues,
        and we also add to a list the boxes to be marked as a mine or flag.
        """
        evidences= {}
        undiscovered = []
        evidencesList = []
        for i in range(board.board_width):
            for j in range(board.board_height):
                field_status = board.info_map[j, i]
                if field_status in range(1,8):
                    evidences["X" + str(i) + str(j)] = 0
                    evidences["Y" + str(i) + str(j)] = field_status
                elif field_status == 0:
                    evidences["X" + str(i) + str(j)] = 0
                    evidences["Y" + str(i) + str(j)] = 0
                elif field_status == 11:
                    evidencesList.append("Y" + str(i) + str(j))
                    undiscovered.append("X" + str(i) + str(j))
                elif field_status == 9:
                    evidences["X" + str(i) + str(j)] = 1

        print("")
        print("△ evidence discovered after the click  -----------------------------")
        print(" ◻︎ Number of evidences : %d" % len (evidences))
        print("")
        print("")
        print("-------  △  -- "+bcolors.OKBLUE+" CALCULATING NEXT MOVEMENT"+bcolors.ENDC+"  --  △   ---------------------------------")
        print("---------------------  "+bcolors.OKBLUE+"  Please wait "+bcolors.ENDC+"   ------------------------------------------")
        print("")

        finalProbsList = []
        #Evidence reduction:
        checkboxesToIterate = []
        for e in range(len(list(evidences.keys()))):
            #Take out the neighbors of each of the evidences.
            ke = list(evidences.keys())[e][1:2]
            le = list(evidences.keys())[e][2:3]
            listaVecinosEvidencia = game.neightbours_of_position(int(ke),int(le))
            for vesii in listaVecinosEvidencia:
                if vesii not in list(evidences.keys()):
                    checkboxesToIterate.append(vesii)

        checkboxesToIterateSet = list(set(checkboxesToIterate))
        if (reverse is False):
            rr = reversed(checkboxesToIterateSet)
            checkboxesToIterateSet = list(rr)
            reverse = True
        else:
            reverse =  False
        
        """
        The irrelevant nodes are discarded, we keep the X value of the box to iterate and with the probValues X and Y of
        the evidences discovered.

        """
        for u in checkboxesToIterateSet:
            if u in checkedBoxes:
                checkboxesToIterateSet.remove(u)
        
        print("check boxes to loop before iteration")
        print(checkboxesToIterateSet)

        for p in range(len(checkboxesToIterateSet)):
            print("probability of the box: " + checkboxesToIterateSet[p])
            modelCopy = modelo.copy()
            discardedNodes = []
            noBorrar =[]
            kee = checkboxesToIterateSet[p][1:2]
            lee = checkboxesToIterateSet[p][2:3]
            listaVecinosNode_query = game.neightbours_of_position(int(kee),int(lee))
            noBorrar.append("Y"+str(kee)+str(lee))
            noBorrar.append(checkboxesToIterateSet[p])
            discardedNodes=[]
            contadorEvideciasVecinos = 0

            for vesiii in listaVecinosNode_query:
                if vesiii in list(evidences.keys()):
                    contadorEvideciasVecinos = contadorEvideciasVecinos + 1
                    ke = vesiii[1:2]
                    le = vesiii[2:3]
                    noBorrar.append("Y"+str(ke)+str(le))
                noBorrar.append(vesiii)
            print()
            print("number of evidences in this iteration:")
            print(contadorEvideciasVecinos)
            discovered = False
            if contadorEvideciasVecinos < 1:
                continue

            for y in modelCopy.nodes():
                if y not in noBorrar and y not in evidences.keys():
                    discardedNodes.append(y)

            modelCopy.remove_nodes_from(discardedNodes)
            Model_Game_ev = pgmi.VariableElimination(modelCopy)

            print("TEST")
            node_query = Model_Game_ev.query([checkboxesToIterateSet[p]], evidences)
            if not (checkboxesToIterateSet[p] in node_query.variables):
                continue
                
            assert(len(node_query.variables) == 1)
            print()
            print("[P. !MINE - P. MINE]", flush=True)
            print(node_query.values)

            probValues = node_query.values
            finalProbsList.append(probValues)
            """
            If we are sure that there is a mine, we mark it with a flag directly and they are calculations that we will not have
            to repeat afterwards. 
            """
            if probValues[1] >= 0.790:
                game.play_move("flag",int(kee),int(lee))
                checkedBoxes.append("X"+str(kee)+str(lee))
                print()
                print("¡¡ Found mine !!")
                print()
                game.print_board()
                # if game.game_status == 1:
                    # sys.exit()
                continue

            if probValues[0] >= 0.90:
                """
                If we are certain that there is no mine in that box, we click directly to continue with another iteration of the game
                """
                valorReal = 1 - probValues[0]
                print("It has been discovered that the box " + checkboxesToIterateSet[p] + " is the least likely to contain a mine, specifically: " + str(valorReal))
                print("Click en: "+str(kee)+","+str(lee))
                game.play_move("click",int(kee),int(lee))
                game.print_board()
                board = game.board
                print("----------------------------------------------------------------------------------------------------------------------")
                discovered = True
            
                break

        if discovered is False:
            if not finalProbsList or len(finalProbsList) == 0:
                print("Little evidence, retry")
                break
            listasCeros = [item[0] for item in finalProbsList]
            con_bombas = [item[1] for item in finalProbsList]
            for h in range(len(con_bombas)):
                """
                If during the iteration we have not been able to safely mark any box as mine, we mark it now.
                """                
                if con_bombas[h] >= .800:
                    elemento = checkboxesToIterateSet[h]
                    ke = elemento[1:2]
                    le = elemento[2:3]
                    game.play_move("flag",int(ke),int(le))
                    checkedBoxes.append("X"+str(ke)+str(le))
                    print()
                    print("!! Found mine !!")
                    print()
                    game.print_board()
                    print("Checked Boxes")
                    print(checkedBoxes)
    
            if game.game_status == 1:
                print("")
                game.print_board()
                # sys.exit()
            else:
                """
                If during the iteration of the cells to discover, we have not been able to click with much certainty on a box,
                We will choose the one that is least likely to contain a mine from among all those calculated, and this will be the box chosen for
                make the next click.
                """
                maximo = numpy.amax(listasCeros)
                winner = checkboxesToIterateSet[listasCeros.index(maximo)]
                print(winner)
                res = 1 - maximo
                print("Se ha discovered que la casilla " + winner + " es la que menos posibilidades tiene de contener una mina, en concreto: " + str(res))
                tt = winner[1:2]
                ss = winner[2:3]
                print("Click en: "+str(tt)+","+str(ss))
                game.play_move("click",int(tt),int(ss))
                game.print_board()
                board = game.board
                print("----------------------------------------------------------------------------------------------------------------------")
            
    return game.game_status



        
