'use strict';

import gulp from 'gulp';
import gulpLoadPlugins from 'gulp-load-plugins';
import del from 'del';
import runSequence from 'run-sequence';

const $ = gulpLoadPlugins();
const imagemin = require('gulp-imagemin');
const imageminMozjpeg = require('imagemin-mozjpeg');

const paths = {
  main: 'src/main',
  tmp: 'build/.tmp',
  dist: {
    css : 'src/main/resources/static/css',
    images : 'src/main/resources/static/images'
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
    .pipe(gulp.dest(`${paths.dist.images}/2`))
);

// Copy over the scripts that are used in importScripts as part of the generate-service-worker task.
gulp.task('vendors', () => {
  return gulp.src(paths.vendors)
    .pipe(gulp.dest(`${paths.dist}/js`));
});

// Clean output directory
gulp.task('clean', () => del([paths.tmp, `${paths.dist.images}`], {dot: true}));

// Watch files for changes
gulp.task('watch', ['default'], () => {
  gulp.watch([`${paths.main}/sass/**/*.scss`], ['styles']);
});

// Build production files, the default task
gulp.task('default', ['clean'], cb =>
  runSequence(
    ['styles', 'images'],
    cb
  )
);

