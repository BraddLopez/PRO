module.exports = {
  env: {
    node: true,  // Especifica que estamos trabajando en un entorno Node.js
    es6: true,   // Permite el uso de características modernas de ECMAScript
  },
  parserOptions: {
    ecmaVersion: 2018,  // Configura la versión de ECMAScript
  },
  extends: [
    "eslint:recommended",  // Usa las reglas recomendadas por ESLint
    "plugin:node/recommended"  // Usa las reglas recomendadas para Node.js
  ],
  rules: {
    "no-restricted-globals": ["error", "name", "length"],
    "prefer-arrow-callback": "error",
    "quotes": ["error", "double", {"allowTemplateLiterals": true}],
  },
  globals: {
    module: "readonly",  // Define que 'module' está permitido en el entorno Node.js
    require: "readonly",  // Define que 'require' está permitido en Node.js
    __dirname: "readonly",  // Define '__dirname' como global
    process: "readonly",  // Define 'process' como global
  },
};

