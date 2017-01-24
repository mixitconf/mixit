'use strict';

import gulp from 'gulp';
import gulpLoadPlugins from 'gulp-load-plugins';
import del from 'del';
import runSequence from 'run-sequence';

const $ = gulpLoadPlugins();
const imagemin = require('gulp-imagemin');
const imageminMozjpeg = require('imagemin-mozjpeg');
const named = require('vinyl-named');
const webpack = require('webpack-stream');

const paths = {
  main: 'src/main',
  tmp: 'build/.tmp',
  vendors: [
    'node_modules/foundation/js/foundation-sites/dist/js/foundation.js',
    'node_modules/fastclick/lib/fastclick.js',
    'node_modules/jquery/dist/jquery.js'
  ],
  dist: {
    css : 'build/resources/main/static/css',
    images : 'build/resources/main/static/images',
    js: 'build/resources/main/static/js'
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
    .pipe(imagemin([imagemin.gifsicle(), imageminMozjpeg(), imagemin.optipng(), imagemin.svgo()], {
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
gulp.task('ts-to-js', ['ts'], () => gulp
  .src(`${paths.tmp}/ts/app.js`)
  .pipe(named())
  .pipe(webpack())
  .pipe($.sourcemaps.init())
  .pipe($.uglify({preserveComments: 'none'}))
  .pipe($.sourcemaps.write('.'))
  .pipe(gulp.dest(`${ paths.dist.js}`))
);

// Clean output directory
gulp.task('clean', () => del([paths.tmp, paths.dist.images, paths.dist.js], {dot: true}));

// Watch files for changes
gulp.task('watch', ['default'], () => {
  gulp.watch([`${paths.main}/sass/**/*.scss`], ['styles']);
  gulp.watch([`${paths.main}/ts/**/*.ts`], ['ts-to-js']);
});

// Build production files, the default task
gulp.task('default', ['clean'], cb =>
  runSequence(
    ['styles', 'images', 'js-vendors', 'ts-to-js'],
    cb
  )
);

