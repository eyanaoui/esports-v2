from sales_prediction_model import run_sales_prediction
from recommendation_model import run_recommendation_model


def main():
    print("====================================")
    print(" E-SPORTS ML PIPELINE STARTED")
    print("====================================")

    run_sales_prediction()
    run_recommendation_model()

    print("====================================")
    print(" E-SPORTS ML PIPELINE FINISHED")
    print("====================================")


if __name__ == "__main__":
    main()