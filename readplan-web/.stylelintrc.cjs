module.exports = {
  extends: ['stylelint-config-standard-scss', 'stylelint-config-recommended-vue/scss'],
  overrides: [
    {
      files: ['**/*.vue'],
      customSyntax: 'postcss-html',
    },
  ],
  rules: {
    'selector-class-pattern': null,
    'no-descending-specificity': null,
  },
};
