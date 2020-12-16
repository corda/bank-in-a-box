import { Card, CardContent, Divider, Fade, Paper, Typography } from '@material-ui/core';

import React from 'react';
import { RecurringPayment } from '../../store/types';
import { useInfoDisplayStyles } from '../MaterialStyles/InfoDisplayStyles';
import { useTranslation } from 'react-i18next';

type Props = {
    recurringPayment: RecurringPayment;
};

const RecurringPaymentDetails: React.FC<Props> = (props) => {
    const { t } = useTranslation('common');
    const { recurringPayment } = props;
    const infoDisplayStyles = useInfoDisplayStyles();

    return (
        <Fade in={true}>
            <Paper elevation={3} className={infoDisplayStyles.infoDisplay}>
                <div className={infoDisplayStyles.column}>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:recurringPayment.id')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {recurringPayment.recurringPaymentId}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:transaction.amount')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {recurringPayment.amount}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:recurringPayment.error')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {recurringPayment.error?.length > 0
                                    ? recurringPayment.error
                                    : t('common:recurringPayment.succesfulTransaction')}
                            </Typography>
                        </CardContent>
                    </Card>
                </div>
                <div className={infoDisplayStyles.column}>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:recurringPayment.paymentDate')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {recurringPayment.txDate}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:recurringPayment.iterationsLeft')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {recurringPayment.iterationNum}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:recurringPayment.period')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {recurringPayment.period}
                            </Typography>
                        </CardContent>
                    </Card>
                </div>
            </Paper>
        </Fade>
    );
};

export default RecurringPaymentDetails;
