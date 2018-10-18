'use strict';

const gulp = require('gulp');
const del = require('del');
const wbBuild = require('workbox-build');
const $ = require('gulp-load-plugins')();
const named = require('vinyl-named');
const webpack = require('webpack-stream');
const ts = require("gulp-typescript");

const paths = {
  main: 'src/main',
  tmp: 'build/.tmp',
  vendors: [
    'node_modules/foundation-sites/dist/js/foundation.js',
    'node_modules/jquery/dist/jquery.js'
  ],
  dist: {
    css: 'build/resources/main/static/css',
    images: 'build/resources/main/static/images',
    js: 'build/resources/main/static/js',
    resources: 'build/resources/main',
  }
};

const AUTOPREFIXER_BROWSERS = [
  'ie >= 10',
  'ie_mob >= 10',
  'ff >= 30',
  'chrome >= 34',
  'safari >= 6',
  'opera >= 23',
  'ios >= 7',
  'android >= 4.4',
  'bb >= 10'
];

// Compile and automatically prefix stylesheets
gulp.task('styles', () =>
  gulp.src([`${paths.main}/sass/**/*.scss`])
      .pipe($.newer(`${paths.tmp}/styles`))
      .pipe($.sourcemaps.init())
      .pipe($.sass({
                     precision: 10
                   }).on('error', $.sass.logError))
      .pipe($.autoprefixer(AUTOPREFIXER_BROWSERS))
      .pipe(gulp.dest(`${paths.tmp}/styles`))
      .pipe($.if('*.css', $.cssnano()))
      .pipe($.size({title: 'styles'}))
      .pipe($.sourcemaps.write('./'))
      .pipe(gulp.dest(`${paths.dist.css}`))
);


// We use webp format for Chrome users
gulp.task('images-webp', () =>
  gulp.src(`${paths.main}/images/**/*.{svg,png,jpg}`)
      .pipe($.webp())
      .pipe(gulp.dest(`${paths.dist.images}`))
);

gulp.task('images-copy', () =>
  gulp.src('build/.tmp/img/**/*.{svg,png,jpg,webp}')
      .pipe(gulp.dest(`${paths.dist.images}`))
);

gulp.task('images', gulp.parallel('images-webp', 'images-copy'));

// Copy over the scripts that are used in importScripts as part of the generate-service-worker task.
gulp.task('js-vendors', () => {
  return gulp.src(paths.vendors)
             .pipe($.sourcemaps.init())
             .pipe($.uglify())
             .pipe($.sourcemaps.write('.'))
             .pipe(gulp.dest(`${paths.dist.js}`));
});


// For the moment we keep one file by ts file
const TYPESCRIPT_OPTION = {
  noImplicitAny: true,
  module: "system",
  moduleResolution: "node",
  sourceMap: true,
  target: "es6",
  lib: [
    "es2017",
    "dom"
  ]
};

gulp.task("ts-to-js", () =>
  gulp.src(`${paths.main}/ts/**/*.ts`)
      .pipe(ts(TYPESCRIPT_OPTION))
      .js
      .pipe(gulp.dest(`${paths.tmp}/ts`))
);

// Compile the type script files in javascript
gulp.task('js-to-es5', () =>
  gulp.src(`${paths.tmp}/ts/*.js`)
      .pipe(named())
      .pipe(webpack( { mode: 'production'}))
      .pipe($.sourcemaps.init())
      //.pipe($.uglify())
      .pipe($.sourcemaps.write('.'))
      .pipe(gulp.dest(`${paths.dist.js}`))
);

gulp.task('js-custom', gulp.series('js-to-es5', 'ts-to-js'));

gulp.task('copy-templates', () => {
  return gulp.src(['src/main/resources/templates/**'])
             .pipe(gulp.dest(`${paths.dist.resources}/templates`));
});

gulp.task('copy-messages', () => {
  return gulp.src(['src/main/resources/messages*.properties'])
             .pipe(gulp.dest(`${paths.dist.resources}`));
});


const SERVICE_WORKER_PARAMS = {
  swSrc: `${paths.main}/resources/static/sw.js`,
  swDest: `${paths.tmp}/sw.js`,
  globDirectory: `${paths.dist.resources}/static`,
  globPatterns: ['**\/*.{js,html,css,svg}']
};

gulp.task('service-worker-bundle', () =>
  wbBuild.injectManifest(SERVICE_WORKER_PARAMS).catch((err) => console.log('[ERROR] This happened: ' + err))
);

gulp.task('service-worker-optim', () =>
  gulp.src(`build/.tmp/sw.js`)
      .pipe($.sourcemaps.init())
      .pipe($.sourcemaps.write())
      .pipe($.uglify())
      .pipe($.size({title: 'scripts'}))
      .pipe($.sourcemaps.write('.'))
      .pipe(gulp.dest(`build/dist`))
);

gulp.task('service-worker-resource', (cb) =>
  gulp.src(['node_modules/workbox-sw/build/*-sw.js'])
      .pipe($.size({title: 'copy', showFiles: true}))
      .pipe(gulp.dest(`${paths.dist.resources}/static`))
      .on('end', () => cb()));

gulp.task('service-worker', gulp.series('service-worker-bundle', 'service-worker-optim', 'service-worker-resource'));


// Clean output directory
gulp.task('clean', () => del([paths.tmp, paths.dist.images, paths.dist.js], {dot: true}));

// Watch files for changes
gulp.task('watch-styles', () => gulp.watch([`${paths.main}/sass/**/*.scss`], gulp.series('styles')));
gulp.task('watch-ts', () => gulp.watch([`${paths.main}/ts/**/*.ts`], gulp.series('js-custom')));
gulp.task('watch-templates', () => gulp.watch([`${paths.main}/resources/templates/**`], gulp.series('copy-templates')));
gulp.task('watch-resources', () => gulp.watch([`${paths.main}/resources/messages*.properties`], gulp.series('copy-messages')));


gulp.task('dev', gulp.series('styles', 'images', 'js-vendors', 'js-custom', 'service-worker'));
gulp.task('watch', gulp.series('dev', 'watch-styles', 'watch-ts', 'watch-templates', 'watch-resources'));
gulp.task('build', gulp.series('dev'));
gulp.task('default', gulp.series('build'));

