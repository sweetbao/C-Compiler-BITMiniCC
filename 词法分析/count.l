%{
      int num_lines = 0;
        num_chars = 0;
%}
%%
\n      ++num_lines; ++num_chars;
.       ++num_chars;

%%

main( argc, argv )
int argc;
char **argv;
{
    ++argv, --argc;  /* skip over program name */
    if ( argc > 0 )
            yyin = fopen( argv[0], "r" );
    else
            yyin = stdin;

   	yylex();
    printf( "# of lines = %d, # of chars = %d\n",num_lines, num_chars );
                 
}

int yywrap()
{ 
  return 1;
}
