gnuplot_args=-e "width=800;height=900;fontname='Verdana'; fontsize='26'; color_1='\#222222'; color_2='\#666666'; color_3='\#AAAAAA'; color_4='\#000000'"

run:
	cd sampling_optimization && gnuplot $(gnuplot_args) -p *.plt
	cd product_evaluation && gnuplot $(gnuplot_args) -p *.plt
	cd product_feature_selection && gnuplot $(gnuplot_args) -p *.plt
	cd product_amazon_translate && gnuplot $(gnuplot_args) -p *.plt
	cd compare_demand_classifiers &&gnuplot $(gnuplot_args) -p *.plt

convert:
	inkscape product_eval.svg -E product_eval.eps
	inkscape product_translate_amazon_with_none.svg -E product_translate_amazon_with_none.eps
	inkscape product_translate_amazon_without_none.svg -E product_translate_amazon_without_none.eps
	inkscape product_feature_selection_with_none.svg -E product_feature_selection_with_none.eps
	inkscape product_feature_selection_without_none.svg -E product_feature_selection_without_none.eps
	inkscape sampling_optimization.svg -E sampling_optimization.eps
	inkscape compare_demand_classifiers.svg -E compare_demand_classifiers.eps
	mv *.eps ../figures

