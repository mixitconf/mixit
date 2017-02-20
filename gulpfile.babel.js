'use strict';

import gulp from 'gulp';
import gulpLoadPlugins from 'gulp-load-plugins';
import runSequence from 'run-sequence';
import swPrecache from 'sw-precache';

const $ = gulpLoadPlugins({
  pattern: ['gulp-*', 'del']
});

const imageminMozjpeg = require('imagemin-mozjpeg');
const named = require('vinyl-named');
const webpack = require('webpack-stream');

const paths = {
  main: 'src/main',
  tmp: 'build/.tmp',
  vendors: [
    'node_modules/foundation/js/foundation-sites/dist/js/foundation.js',
    'node_modules/fastclick/lib/fastclick.js',
    'node_modules/jquery/dist/jquery.slim.js'
  ],
  dist: {
    sw: 'build/resources/main/static',
    css : 'build/resources/main/static/css',
    images : 'build/resources/main/static/images',
    js: 'build/resources/main/static/js',
    resources: 'build/resources/main',
  }
};

// Compile and automatically prefix stylesheets
gulp.task('styles', () => {
  const AUTOPREFIXER_BROWSERS = [
    'ie >= 10',
    'ie_mob >= 10',
    'ff >= 30',
    'chrome >= 34',
    'safari >= 7',
    'opera >= 23',
    'ios >= 7',
    'android >= 4.4',
    'bb >= 10'
  ];

  return gulp.src([`${paths.main}/sass/**/*.scss`])
    .pipe($.newer(`${paths.tmp}/styles`))
    .pipe($.sourcemaps.init())
    .pipe($.sass())
    .pipe($.autoprefixer(AUTOPREFIXER_BROWSERS))
    .pipe(gulp.dest(`${paths.tmp}/styles`))
    .pipe($.if('*.css', $.cssnano()))
    .pipe($.size({title: 'styles'}))
    .pipe($.sourcemaps.write('./'))
    .pipe(gulp.dest(`${paths.dist.css}`));
});


// Minimize images
gulp.task('images-min', () =>
  gulp.src(`${paths.main}/images/**/*.{svg,png,jpg}`)
    .pipe($.imagemin([$.imagemin.gifsicle(), imageminMozjpeg(), $.imagemin.optipng(), $.imagemin.svgo()], {
      progressive: true,
      interlaced: true,
      arithmetic: true,
    }))
    .pipe($.size({title: 'images-min', showFiles: true}))
    .pipe(gulp.dest(`${paths.dist.images}`))
);
gulp.task('images', ['images-min'], () =>
  gulp.src(`${paths.dist.images}/**/*.{svg,png,jpg}`)
    .pipe($.webp())
    .pipe($.size({title: 'images-webp', showFiles: true}))
    .pipe(gulp.dest(`${paths.dist.images}`))
);

// Copy over the scripts that are used in importScripts as part of the generate-service-worker task.
gulp.task('js-vendors', () => {
  return gulp.src(paths.vendors)
    .pipe($.sourcemaps.init())
    .pipe($.uglify({preserveComments: 'none'}))
    .pipe($.sourcemaps.write('.'))
    .pipe(gulp.dest(`${paths.dist.js}`));
});

// Compile the type script files in javascript
gulp.task('ts', () => gulp
  .src(`${paths.main}/ts/**/*.ts`, {base: `${paths.main}/ts`})
  .pipe($.newer(`${paths.tmp}/ts`))
  .pipe($.tsc({
    "compilerOptions": {
      "module": "commonjs",
      "moduleResolution": "classic",
      //"outDir": paths.dist.js,
      "noImplicitAny": true,
      "strictNullChecks": true,
      "emitDecoratorMetadata": true,
      "experimentalDecorators": true,
      "sourceMap": true
    },

    "exclude": [
      "node_modules"
    ]
  }))
  .pipe(gulp.dest(`${paths.tmp}/ts`))
);

// Compile the type script files in javascript
gulp.task('ts-to-js', ['ts'], () =>
  gulp.src(`${paths.tmp}/ts/*.js`)
      .pipe(named())
      .pipe(webpack())
      .pipe($.sourcemaps.init())
      .pipe($.uglify({preserveComments: 'none'}))
      .pipe($.sourcemaps.write('.'))
      .pipe(gulp.dest(`${paths.dist.js}`))
);

// Copy over the scripts that are used in importScripts as part of the generate-service-worker task.
function writeServiceWorkerFile(handleFetch, callback) {
  var config = {
    cacheId: 'MiXiT',
    // Determines whether the fetch event handler is included in the generated service worker code. It is useful to
    // set this to false in development builds, to ensure that features like live reload still work. Otherwise, the content
    // would always be served from the service worker cache.
    handleFetch: handleFetch,
    logger: $.util.log,
    runtimeCaching: [{
      urlPattern: '/(.*)',
      handler: 'networkFirst',
      options : {
        networkTimeoutSeconds: 3,
        maxAgeSeconds: 600
      }
    }],
    staticFileGlobs: [ paths.dist.sw + '/**/*.{js,html,css,png,jpg,json,gif,svg,webp,eot,ttf,woff,woff2}'],
    stripPrefix: paths.dist.sw,
    verbose: true
  };

  swPrecache.write(`${paths.tmp}/service-worker.js`, config, callback);
}

gulp.task('generate-service-worker-dev', function(callback) {
  writeServiceWorkerFile(false, callback);
});

gulp.task('generate-service-worker', function(callback) {
  writeServiceWorkerFile(true, callback);
});

gulp.task('copy-templates', () => {
  return gulp.src(['src/main/resources/templates/**'])
    .pipe(gulp.dest(`${paths.dist.resources}/templates`));
});

gulp.task('copy-messages', () => {
  return gulp.src(['src/main/resources/messages*.properties'])
    .pipe(gulp.dest(`${paths.dist.resources}`));
});

gulp.task('package-service-worker', () =>
  gulp.src(`${paths.tmp}/service-worker.js`)
    .pipe($.sourcemaps.init())
    .pipe($.sourcemaps.write())
    .pipe($.uglify({preserveComments: 'none'}))
    .pipe($.size({title: 'scripts'}))
    .pipe($.sourcemaps.write('.'))
    .pipe(gulp.dest(`${paths.dist.sw}`))
);

// Clean output directory
gulp.task('clean', () => $.del([paths.tmp, paths.dist.images, paths.dist.js], {dot: true}));

// Watch files for changes
gulp.task('watch', ['dev'], () => {
  gulp.watch([`${paths.main}/sass/**/*.scss`], ['styles']);
  gulp.watch([`${paths.main}/ts/**/*.ts`], ['ts-to-js']);
  gulp.watch([`${paths.main}/resources/templates/**`], ['copy-templates']);
  gulp.watch([`${paths.main}/resources/messages*.properties`], ['copy-messages']);
});

// Buil dev files
gulp.task('dev', ['clean'], cb =>
  runSequence(
    ['styles', 'images', 'js-vendors', 'ts-to-js'],
    'generate-service-worker-dev',
    'package-service-worker',
    cb
  )
);

// Build production files, the default task
gulp.task('build', cb =>
  runSequence(
    ['styles', 'images', 'js-vendors', 'ts-to-js'],
    'generate-service-worker',
    'package-service-worker',
    cb
  )
);

// Build production files, the default task
gulp.task('default', ['clean'], cb =>
runSequence(
  'build',
  cb
)
);
