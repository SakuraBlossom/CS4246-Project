import sys
import os
import autosolver as aut
import time
import datetime
from tqdm import tqdm
from functools import wraps
from itertools import cycle
from msboard import bcolors



testset = ([5,5,5],[5,5,6],[5,5,7],[8,8,13],[8,8,14],[8,8,15],[10,10,25])

"""

Loop through the given test array [size, size, numberBombs], create the game and the Bayesian network to solve it automatically,
until the game state is 0 (won), it does not proceed to the next test case.

Parameters
----------
test: list, array-like
    test cases to be carried out
"""

def run(test : list):
    for i in tqdm(range(0,len(test)-1)):
        success = False
        tries = 0
        while not success and tries < 5:
            print()
            print(bcolors.WARNING+"Test " + str(i+1) + " of " + str(len(test))+"."+bcolors.ENDC)
            print()
            orig_stdout = sys.stdout
            print(bcolors.OKBLUE+'Generating board:  '+bcolors.ENDC+str(test[i][0])+' x '+str(test[i][1])+' with '+str(test[i][2])+' mines')
            print('...')
            print(bcolors.OKBLUE+'Solving board: '+bcolors.ENDC+str(test[i][0])+' x '+str(test[i][1])+' with '+str(test[i][2])+' mines')
            f = open('Test_'+str(test[i][0])+'x'+str(test[i][1])+'_'+str(test[i][2])+'_mines'+'.txt', 'w')
            sys.stdout = f
            start_time = time.time()
            status = aut.autosolver(test[i][0],test[i][1],test[i][2])
            elapsed_time = time.time() - start_time
            print(bcolors.OKBLUE+"Time spent: "+bcolors.ENDC+str(datetime.timedelta(seconds=elapsed_time)))
            print()
            sys.stdout = orig_stdout
            f.close()
            
            if status == 0:
                success = False
                tries += 1
                print()
                print(bcolors.FAIL+" Lost game, retrying... "+bcolors.ENDC)
                print()
                print(bcolors.OKBLUE+"Time spent: "+bcolors.ENDC+str(datetime.timedelta(seconds=elapsed_time)))
                print()
                print("-----------------------------------------------------------------------------------------------------")
            else:
                success = True
                print()
                print(bcolors.OKGREEN+" ! Game won ! "+bcolors.ENDC)
                print()
                print(bcolors.OKBLUE+"Time spent: "+bcolors.ENDC+str(datetime.timedelta(seconds=elapsed_time)))
                print()
                print("-----------------------------------------------------------------------------------------------------")

run(testset)