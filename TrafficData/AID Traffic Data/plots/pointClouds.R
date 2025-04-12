library(ggplot2)

data0 <- read.table("../I35E - 2447/20130603.txt", header=F, sep=" ")

sp <- ggplot(data=data0, aes(x=data0$V5, y=data0$V4)) + geom_point()
#sp = sp + geom_hline(yintercept=85, linetype="dashed", color = "red") 
sp = sp + xlab("Occupancy") + ylab("Speed") 
sp = sp + geom_smooth(span = 0.2)
sp

summary(data0$V4)