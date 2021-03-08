// Ensure babel transpilations use the right browserslist environment. By default
// it will use NODE_ENV (equal to `test`), and because that env is not defined in
// `package.json`, it will use browserslist defaults.
process.env.BROWSERSLIST_ENV = 'production';
