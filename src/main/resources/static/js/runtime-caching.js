(function(global) {
  global.toolbox.router.get('/(.*)', global.toolbox.networkFirst, {});
})(self);
