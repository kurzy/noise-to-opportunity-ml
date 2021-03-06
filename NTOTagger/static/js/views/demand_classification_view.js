define([
	'jquery',
	'underscore',
	'backbone',
	'text!views/demand_classification_view.html'
	], function($, _, Backbone, DemandClassificationViewTemplate) {

		var Post = Backbone.Model.extend({
			defaults: {
				id: -1,
				title: "No more posts",
				text: "No more posts are available for tagging. You finished. Yeah!",
			}
		});
		var Posts = Backbone.Collection.extend({
			model: Post
		});

		return Backbone.View.extend({
			template: _.template(DemandClassificationViewTemplate),

			keyMapping: {
				121: "#btn-has-demand",
				110: "#btn-has-no-demand",
				105: "#btn-has-no-idea",
				49:  "#btn-category-1",
				50:  "#btn-category-2",
				51:  "#btn-category-3",
				52:  "#btn-category-4",
				53:  "#btn-category-5"
			},
			

			events: {
				"click .btn-tag":         			"tagPost",

				"click .btn-demand":				"showCategoryButtons",
				"click .btn-category":              "showNewPost",
				"click .btn-next":                  "showNewPost",
				"keydown":                          "keyAction",
				"change .tagger-name":				"changeTaggerName"
			},

			initialize: function(options) {
				_.bindAll(this, "render", "keyAction");

				this.route = options.route;
				this.currentPost = new Post;
				this.posts = new Posts;
				this.posts.on("add remove", this.render);

				this.localStorage = localStorage || {}

				$(document).bind('keypress', this.keyAction);

				if (options.postId) {
					this.loadPost(options.postId);
				} else {
					this.loadPostsFromRoute();
				}
				this.render();
			},

			render: function() {
				// check if first element of collection changed, if yes, render
				if (this.posts.size() === 0) {
					this.$el.html(this.template({ post: new Post().attributes }));
					return;
				}
				var firstPost = this.posts.at(0);
				if (firstPost.id === this.currentPost.id)
					return;

				this.currentPost = firstPost;
				this.$el.html(this.template({ post: firstPost.attributes, route:this.route }));
				Backbone.history.navigate(this.route + "/" + firstPost.id, {replace: true});
				console.log("Rendering next post: " + firstPost.id + ".");

				// precache next posts if necessary
				if (this.posts.size() <= 5)
					this.loadPostsFromRoute();
			},

			keyAction: function(event) {
				var code = (event.keyCode || event.which);
				this.$el.find(this.keyMapping[code]).click();
			},

			showCategoryButtons: function() {
				this.$el.find(".demand-decision").hide();
				this.$el.find(".category-decision").show();
			},

			tagPost: function(event) {
				var data = $(event.target).data();
				data.tagger = localStorage.tagger
				$.post('classify_post/' + this.currentPost.id, data);
			},

			showNewPost: function() {
				// remove first post from collection, automatically triggers rerendering
				this.posts.shift();
			},


			/*
			 * AJAX calls to load posts
			 */
			// Loads a single post with a given postId.
			loadPost: function(postId) {
				var self = this;
				$.get("/post/" + postId, function(data) {
					data = JSON.parse(data);
					self.posts.add(data.posts);
				});
			},

			// Loads next posts from specified route.
			loadPostsFromRoute: function() {
				var self = this;
				$.get("/" + this.route, {tagger: localStorage.tagger}, function(data) {
					data = JSON.parse(data);
					if (data.posts.length === 0) {
						console.log("No more posts.");
						return;
					}
					console.log("Loaded " + data.posts.length + " more posts.");
					self.posts.add(data.posts);
				})
			},

			changeTaggerName:function(){
				this.localStorage.taggerName = this.$el.find('#tagger-name').val();
				console.log(this.localStorage.taggerName);
			}


	});
});
