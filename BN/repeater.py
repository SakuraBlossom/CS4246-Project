import sys
import os
import autosolver as aut
import time
import datetime
from tqdm import tqdm
from functools import wraps
from itertools import cycle
from msboard import bcolors



testset = [
    [8,8,6],[8,8,13],[8,8,19],[8,8,26],
    [10,10,10],[10,10,20],[10,10,30],[10,10,40],
    [20,20,10],[20,20,20],[20,20,30],[20,20,40],[20,20,80],
]

"""

Loop through the given test array [size, size, numberBombs], create the game and the Bayesian network to solve it automatically,
until the game state is 0 (won), it does not proceed to the next test case.

Parameters
----------
test: list, array-like
    test cases to be carried out
"""

def run(test : list, num_trials : int=1):
    test_res = list()
    for i in tqdm(range(0,len(test)-1)):
        for trial_idx in range(num_trials):
            success = False
                
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
            status = None
            try:
                status = aut.autosolver(test[i][0],test[i][1],test[i][2])
            except Exception as e:
                print("type error: " + str(e))
                print(traceback.format_exc())

            elapsed_time = time.time() - start_time
            print(bcolors.OKBLUE+"Time spent: "+bcolors.ENDC+str(datetime.timedelta(seconds=elapsed_time)))
            print()
            sys.stdout = orig_stdout
            f.close()
            
            success = (status == 0)
            elapsed_time_str = str(datetime.timedelta(seconds=elapsed_time))

            if status is None:
                status_str = bcolors.FAIL+" CRASH "+bcolors.ENDC
            elif status == 0:
                status_str = bcolors.OKGREEN+" ! Game won ! "+bcolors.ENDC
            else:
                status_str = bcolors.FAIL+" Lost game "+bcolors.ENDC

            print(status_str)
            print(bcolors.OKBLUE+"Time spent: "+bcolors.ENDC+str(elapsed_time_str))
            print()
            print("-----------------------------------------------------------------------------------------------------")
            test_res.append(f"{test[i][0]}x{test[i][1]} with {test[i][2]} mines - TRY {trial_idx} - {status_str} - TIME: {elapsed_time_str} / {elapsed_time}")
    
        pass

    summary = "\n".join(test_res)
    print(summary)
    return test_res

run(testset, 10)
