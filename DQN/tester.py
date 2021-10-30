import torch
import numpy as np
import sys
sys.path.insert(1,"./Models")
from ddqn import DDQNOld, DDQN
from renderer import Render
from game import MineSweeper
import time
import argparse


### Preferably don't mess with the parameters for now.
### Class takes in only one parameter as initialization, render true or false
class Tester():
    def __init__(self,render_flag, game_width=6, game_height=6, game_mines=6):
        self.model = DDQN(36,36)
        self.render_flag = render_flag
        self.width = game_width
        self.height = game_height
        self.env = MineSweeper(game_width, game_height, game_mines)
        if(self.render_flag):
            self.renderer = Render(self.env.state)
        self.load_models(20000)
    
    def get_action(self,state):
        state = state.flatten()
        mask = (1-self.env.fog).flatten()
        action = self.model.act(state,mask)
        return action

    def load_models(self,number):
        path = "pre-trained/ddqn_dnn"+str(number)+".pth"
        mydict = torch.load(path)
        self.model.load_state_dict(mydict['current_state_dict'])
        self.model.epsilon = 0
    
    def do_step(self,action):
        i = int(action/self.width)
        j = action%self.width

        if(self.render_flag):
            self.renderer.state = self.env.state
            self.renderer.bugfix()

        next_state,terminal,reward = self.env.choose(i,j)

        if self.render_flag:
            self.renderer.draw()

        return next_state,terminal,reward
    
### Tests winrate in "games_no" games
def win_tester(games_no):
    tester = Tester(False)
    state = tester.env.state
    mask = tester.env.fog
    wins = 0
    i=0
    step = 0
    first_loss = 0

    while(i<games_no):
        step+=1
        action = tester.get_action(state)
        next_state,terminal,reward = tester.do_step(action)
        state = next_state
        if(terminal):
            if(step==1 and reward==-1):
                first_loss+=1
                games_no += 1
            i+=1
            tester.env.reset()
            state = tester.env.state
            if(reward==1):
                wins+=1
            step=0
    
    ### First_loss is subtracted so that the games with first pick as bomb are subtracted
    print(f"Total Num of games (inc first step loss): {games_no}")
    print(f"Win Rate (excluding First Loss): {wins*100/(games_no-first_loss)}%")
    print(f"Win Rate: {wins*100/games_no}%")


def slow_tester():
    tester = Tester(True)
    state = tester.env.state
    count = 0
    start = time.perf_counter()
    step = 0
    first_loss = 0
    terminal = False
    reward = 0

    while not terminal:
        count+=1
        step+=1
        action = tester.get_action(state)
        next_state,terminal,reward = tester.do_step(action)
        state = next_state
        print(reward)
        time.sleep(0.5)

    
    if reward == 1:
        print("WIN")
    else:
        print("LOSS")

    time.sleep(5)
    tester.env.reset()
    step=0
    state = tester.env.state
    pass
        
def main(raw_args=None):
    parser = argparse.ArgumentParser()
    parser.add_argument('--display', '-d', action='store_true', default=False, help='Display game')
    parser.add_argument('--games', '-g', type=int, default=1000, help="sets number of games in tester mode")
    parser.add_argument('--size', '-s', type=int, default=8, help="sets size of board")
    parser.add_argument('--mines', '-m', type=int, default=8, help="sets number of mines")
    params = parser.parse_args(raw_args)

    if params.display:
        slow_tester()
    else:
        win_tester(params.games)


if __name__ == "__main__":
    main()


        
