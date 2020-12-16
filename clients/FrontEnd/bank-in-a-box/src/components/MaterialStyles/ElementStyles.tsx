import { Theme, createStyles, makeStyles } from '@material-ui/core';

export const useDropDownStyles = makeStyles((theme: Theme) =>
    createStyles({
        menuItem: {
            display: 'block',
        },
    })
);
