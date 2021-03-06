#
# basic product classifier evaluation
#

# set terminal wxt size 800,700
set terminal svg size width,height fname fontname fsize fontsize
set output '../product_eval.svg'

set xrange [-0.4:3.9]
set yrange [0:115]
# set xlabel "Group size or window size"
set ylabel "Overall Precision in %"
set xtics ("K-Nearest Neighbor" 0.2, "Perceptron" 1.2, "SVM" 2.2, "Logistic" 3.2)
set xtics rotate by -45 offset -1.5
set key right top
set boxwidth 0.4
set style fill solid
set datafile separator ","
plot \
	'./data.csv' every 2::1 using 1:3 with boxes lt rgb color_1 title "Without \"NONE\" Prediction" , \
	'./data.csv' every 2::1 using 1:($3 + 8):3 with labels rotate by 90 notitle, \
	'./data.csv' every 2 		using 1:3 with boxes lt rgb color_2 title "With \"NONE\" Prediction", \
	'./data.csv' every 2 		using 1:($3 + 8):3 with labels rotate by 90 notitle
