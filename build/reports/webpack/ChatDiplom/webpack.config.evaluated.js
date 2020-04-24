{
  mode: 'production',
  resolve: {
    modules: [
      '/home/admin/IdeaProjects/ChatDiplom/build/js/packages/ChatDiplom/kotlin-dce',
      'node_modules'
    ]
  },
  plugins: [],
  module: {
    rules: [
      {
        test: /\.js$/,
        use: [
          'kotlin-source-map-loader'
        ],
        enforce: 'pre'
      }
    ]
  },
  entry: {
    main: [
      '/home/admin/IdeaProjects/ChatDiplom/build/js/packages/ChatDiplom/kotlin-dce/ChatDiplom.js'
    ]
  },
  output: {
    path: '/home/admin/IdeaProjects/ChatDiplom/build/distributions',
    filename: [Function: filename],
    library: 'ChatDiplom',
    libraryTarget: 'umd'
  },
  devtool: 'source-map'
}