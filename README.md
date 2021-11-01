# CS4246-Project
---

MineSweeper AI that beats Mine Sweeper with the least number of moves.

---

## Work Distribution

- DQN - karthigeyan & Shawn
- Naive Bayesian - Shawn


##DQN 
1. Make sure that theres an empty folder "pretrained" in DQN folder or else it will be overwritten if training from scratch
1. Make sure to have an empty log txt or rename any saved log to prevent in from being overwritten under Logs file
1. ***_cpu.py uses cpu only while the other attempts to use both gpu and cpu 
1. Edit line 193 of train_ddqn.py or train_ddqn_cpu.py for grid size and mines
1. Batches are saved every 200 epoch
1. Comment line 68 in train_ddqn.py or train_ddqn_cpu.py if training from scratch
1. To resume training, uncomment line 68 and input the epoch to train from. (check under pre-trained for last saved. )
