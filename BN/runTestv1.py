import sys
import os
import autosolver as aut
import time
import datetime
from tqdm import tqdm
from functools import wraps
from itertools import cycle
from msboard import bcolors



testset = [ [20,20,80] ]

#testset = [ [6,6,6] ]
"""

Loop through the given test array [size, size, numberBombs], create the game and the Bayesian network to solve it automatically,
until the game state is 0 (won), it does not proceed to the next test case.

Parameters
----------
test: list, array-like
    test cases to be carried out
"""

def run(test : list, num_trials : int=1):
    test_cases = list()
    test_res = list()
    for i in tqdm(range(len(test))):

        total_wins = 0
        total_win_testcase_time = 0
        for trial_idx in range(num_trials):

            board_size_str = f"{test[i][0]}x{test[i][1]}"
                
            print()
            print(bcolors.WARNING+f"Test {i+1} of {len(test)} [Trial {trial_idx+1}]."+bcolors.ENDC)
            print()
            orig_stdout = sys.stdout
            print(bcolors.OKBLUE+'Generating board:  '+bcolors.ENDC+board_size_str+' with '+str(test[i][2])+' mines')
            print('...')
            print(bcolors.OKBLUE+'Solving board: '+bcolors.ENDC+board_size_str+' with '+str(test[i][2])+' mines')
            f = open(f'logs/Test_{board_size_str}_{test[i][2]}_mines_2.txt', 'w')
            sys.stdout = f
            start_time = time.time()
            status = None
            
            try:
                status = aut.autosolver(test[i][0],test[i][1],test[i][2])
            except Exception as e:
                print("type error: " + str(e))

            elapsed_time = time.time() - start_time

            print(bcolors.OKBLUE+"Time spent: "+bcolors.ENDC+str(datetime.timedelta(seconds=elapsed_time)))
            print()
            sys.stdout = orig_stdout
            f.close()
            
            elapsed_time_str = str(datetime.timedelta(seconds=elapsed_time))

            if status is None:
                status_str = bcolors.FAIL+" CRASH "+bcolors.ENDC
            elif status == 1:
                total_wins += 1
                total_win_testcase_time += elapsed_time
                status_str = bcolors.OKGREEN+" ! Game won ! "+bcolors.ENDC
            else:
                status_str = bcolors.FAIL+" Lost game "+bcolors.ENDC

            print()
            print(status_str)
            print(bcolors.OKBLUE+"Time spent: "+bcolors.ENDC+str(elapsed_time_str))
            print()
            print("-----------------------------------------------------------------------------------------------------")
            test_cases.append(f"{test[i][0]}x{test[i][1]} with {test[i][2]} mines - TRY {trial_idx} - {status_str} - TIME: {elapsed_time_str} / {elapsed_time}")
    
        avg_elapsed_time = (total_win_testcase_time / total_wins) if total_wins > 0 else 0
        avg_elapsed_time_str = str(datetime.timedelta(seconds=avg_elapsed_time))
        test_res.append(f"{test[i][0]}x{test[i][1]} with {test[i][2]} mines - {num_trials} TRIES - {total_wins} WINS - TIME: {avg_elapsed_time_str} / {avg_elapsed_time}")
    
        print()
        print("\n".join(test_res))
        pass

    print("\n".join(test_cases))
    print()
    print("\n".join(test_res))
    pass

run(testset, 50)
