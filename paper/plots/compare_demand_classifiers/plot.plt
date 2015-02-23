#
# basic product classifier evaluation
#

# set terminal wxt size 800,700
set terminal svg size 800,700 fname fontname fsize fontsize
set output '../compare_demand_classifiers.svg'


set xrange [0:9]
set yrange [70:90]
# set xlabel "Group size or window size"
set ylabel "Overall Precision in %"
# set xtics ("K-Nearest Neighbor" 0.2, "Perceptron" 1.2, "SVM" 2.2, "Logistic" 3.2)
set boxwidth 0.5
set style fill solid
set xtic rotate by 315 offset -1.5
set datafile separator ","
plot \
	'./data.csv' using 1:3 with boxes lt rgb color_1 notitle , \
	'./data.csv' using 1:($3+1.5):3 with labels notitle, \
	'./data.csv' using 1:(0):xticlabel(2) with lines notitle
